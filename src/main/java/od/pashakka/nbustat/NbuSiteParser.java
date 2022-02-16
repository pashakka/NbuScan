package od.pashakka.nbustat;

import ch.qos.logback.classic.Level;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class NbuSiteParser {
    private static final Logger logger = LoggerFactory.getLogger(
            NbuSiteParser.class);
    private final String downloadUrl;
    private final String workPath;
    private final String mainUrl;
    private final String proxy;
    private final int proxyPort;

    public NbuSiteParser(Properties properties) {
        this.mainUrl = properties.getProperty("MainURL");
        this.downloadUrl = properties.getProperty("DownloadURL");
        this.workPath = properties.getProperty("WorkPath");
        this.proxy = properties.getProperty("Proxy");
        this.proxyPort = Integer.parseInt(Optional.ofNullable(properties.getProperty("ProxyPort")).orElse("0"));
        String logLevel = properties.getProperty("LogLevel");

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.valueOf(logLevel));
    }

    public void parse() throws IOException {
        logger.info("Start parsing: {} ...", downloadUrl);
        Document doc;
        try {
            Connection connection = Jsoup.connect(downloadUrl)
                    .userAgent("Chrome/4.0.249.0 Safari/532.5")
                    .referrer("http://www.google.com");
            if (this.proxy != null && !this.proxy.isEmpty()) {
                connection.proxy(this.proxy, this.proxyPort);
            }
            doc = connection.get();
        } catch (IOException e) {
            logger.error("err_load_doc", e);
            throw e;
        }

        Elements box = doc.select("div.box");

        Elements table = box.select("table");

        List<ReportInfo> reportInfos = new ArrayList<>();
        String reportCategory = null;
        String code = null;
        String name = null;
        int trNum = 1;
        for (Element tr : table.select("tr").stream().skip(1).collect(Collectors.toList())) {
            logger.debug("tr:{}", trNum);
            boolean firstRowInCategory = false;
            Elements tds = tr.select("td");

            Element td0 = tds.get(0);
            String td0Text = td0.text();
            logger.debug("\ttd0:{}", td0Text);
            if (!isCode(td0Text) && !isDate(td0Text)) {
                reportCategory = td0Text;
                firstRowInCategory = true;
            }

            List<Element> dataTds;
            if (firstRowInCategory) {
                dataTds = tds.stream().skip(1).collect(Collectors.toList());
            } else {
                dataTds = tds;
            }

            ReportInfo info = new ReportInfo();
            info.setCategory(reportCategory);
            int tdNum = 1;
            while (tdNum <= dataTds.size()) {
                Element td = dataTds.get(tdNum - 1);
                String tdText = td.text();
                logger.debug("\ttd:{}:{}", tdNum, tdText);
                if (isCode(tdText)) {
                    code = tdText.replaceAll("\\*", "");
                } else if (isRepName(tdText)) {
                    name = tdText;
                } else if (isDate(tdText)) {
                    String nextText = dataTds.get(tdNum).text();
                    if (isTextContains(nextText, "Registry")) {
                        info.setRegistryDate(tdText);
                    } else if (isTextContains(nextText, "Domain_List")) {
                        info.setDomainListDate(tdText);
                    } else if (isTextContains(nextText, "Description")) {
                        info.setDescriptionDate(tdText);
                    } else if (isTextContains(nextText, "Controls")) {
                        info.setControlsDate(tdText);
                    }
                } else if (isSchema(tdText)) {
                    info.setSchemaURL(extractURL(td));
                } else if (isTextContains(tdText, "Registry")) {
                    info.setRegistryURL(extractURL(td));
                } else if (isTextContains(tdText, "Domain_List")) {
                    info.setDomainListURL(extractURL(td));
                } else if (isTextContains(tdText, "Description")) {
                    info.setDescriptionURL(extractURL(td));
                } else if (isTextContains(tdText, "Controls")) {
                    info.setControlsURL(extractURL(td));
                }
                tdNum++;
            }

            info.setCode(code);
            info.setName(name);

            reportInfos.add(info);

            trNum++;
        }

        for (ReportInfo reportInfo : reportInfos) {
            updateReportInfo(reportInfo);
            if (reportInfo.isChanged()) {
                logger.info("changes:{}", reportInfo.getChanges());
            }
        }
        logger.info("End parsing: {}", downloadUrl);
    }

    private void updateReportInfo(ReportInfo reportInfo) {
        if (saveFiles(reportInfo, FileType.REGISTRY)) {
            reportInfo.setChangedRegistry();
        }
        if (saveFiles(reportInfo, FileType.DOMAIN_LIST)) {
            reportInfo.setChangedDomainList();
        }
        if (saveFiles(reportInfo, FileType.DESCRIPTION)) {
            reportInfo.setChangedDescription();
        }
        if (saveFiles(reportInfo, FileType.CONTROLS)) {
            reportInfo.setChangedControls();
        }
        saveFiles(reportInfo, FileType.SCHEMA);
    }

    private boolean saveFiles(ReportInfo reportInfo, FileType fileType) {
        String urls = reportInfo.getURL(fileType);
        String date = reportInfo.getDate(fileType);
        boolean isSchema = fileType == FileType.SCHEMA;

        if (urls == null || urls.trim().isEmpty()) return false;

        String path = this.workPath + File.separator +
                "FILES" + File.separator +
                reportInfo.getCategory() + File.separator +
                reportInfo.getCode() + File.separator +
                fileType +
                (isSchema ? "" : File.separator + date);

        boolean newFileDownloaded = false;
        for (String urlStr : urls.split(";")) {
            String fullUrl = this.mainUrl + urlStr;
            String filePath = createFilePath(fullUrl, path);

            if (!isSchema && new File(filePath).exists()) {
                return false;
            }
            downloadUsingNIO(fullUrl, filePath);
            newFileDownloaded = true;
        }

        return newFileDownloaded;
    }

    private String createFilePath(String fullUrl, String path) {
        String fileName = new File(fullUrl).getName();
        return path + File.separator + fileName;
    }

    private void downloadUsingNIO(String urlStr, String file) {
        logger.debug("downloading: {} to {}", urlStr, file);
        URL url;
        try {
            if (file.toUpperCase().contains("XSD")) {
                file = getXsdFileName(file);
            }
            url = new URL(urlStr);

            URLConnection urlConnection;
            if (this.proxy != null && !this.proxy.isEmpty()) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.proxy, this.proxyPort));
                urlConnection = url.openConnection(proxy);
            } else {
                urlConnection = url.openConnection();
            }
            ReadableByteChannel rbc = Channels.newChannel(urlConnection.getInputStream());

            File fileDir = new File(file).getParentFile();
            fileDir.mkdirs();

            FileOutputStream fos = new FileOutputStream(file);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            rbc.close();
        } catch (IOException e) {
            logger.error("err_download_file", e);
            throw new IllegalStateException("err_download_file", e);
        }
    }

    private String getXsdFileName(String file) {
        String xsdFileName = file;
        int i = file.indexOf("?reportdate=");
        if (i > 0) {
            String cleanFile = file.substring(0, i);
            String reportDate = file.substring(file.length() - 8);
            xsdFileName = cleanFile + "_" + reportDate;
        }
        return xsdFileName;
    }

    private String extractURL(Element td) {
        List<String> urls = new ArrayList<>();
        for (Element a : td.select("a")) {
            urls.add(a.attr("href"));
        }

        return String.join(";", urls);
    }

    private boolean isTextContains(String text, String sample) {
        return text.toUpperCase().contains(sample.toUpperCase());
    }

    private boolean isRepName(String text) {
        return text.trim().matches("\".*\"");
    }

    private boolean isSchema(String text) {
        return text.trim().matches("[^.]*x$");
    }

    private boolean isCode(String text) {
        return text.trim().matches("\\w\\wX[^\\s]*");
    }

    private boolean isDate(String text) {
        return text.trim().matches("\\d\\d.\\d\\d.\\d\\d\\d\\d");
    }
}


















