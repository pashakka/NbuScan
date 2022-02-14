package od.pashakka.nbustat;

public class ReportInfo {

    private String category;
    private String code;
    private String name;
    private String registryDate;
    private String registryURL;
    private String domainListDate;
    private String domainListURL;
    private String descriptionDate;
    private String descriptionURL;
    private String controlsDate;
    private String controlsURL;
    private String schemaURL;
    private boolean changedDomainList;
    private boolean changedDescription;
    private boolean changedControls;
    private boolean changedRegistry;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegistryDate() {
        return registryDate;
    }

    public void setRegistryDate(String registryDate) {
        this.registryDate = registryDate;
    }

    public String getRegistryURL() {
        return registryURL;
    }

    public void setRegistryURL(String registryURL) {
        this.registryURL = registryURL;
    }

    public String getDomainListDate() {
        return domainListDate;
    }

    public void setDomainListDate(String domainListDate) {
        this.domainListDate = domainListDate;
    }

    public String getDomainListURL() {
        return domainListURL;
    }

    public void setDomainListURL(String domainListURL) {
        this.domainListURL = domainListURL;
    }

    public String getDescriptionDate() {
        return descriptionDate;
    }

    public void setDescriptionDate(String descriptionDate) {
        this.descriptionDate = descriptionDate;
    }

    public String getDescriptionURL() {
        return descriptionURL;
    }

    public void setDescriptionURL(String descriptionURL) {
        this.descriptionURL = descriptionURL;
    }

    public String getControlsDate() {
        return controlsDate;
    }

    public void setControlsDate(String controlsDate) {
        this.controlsDate = controlsDate;
    }

    public String getControlsURL() {
        return controlsURL;
    }

    public void setControlsURL(String controlsURL) {
        this.controlsURL = controlsURL;
    }

    public String getSchemaURL() {
        return schemaURL;
    }

    public void setSchemaURL(String schemaURL) {
        this.schemaURL = schemaURL;
    }

    @Override
    public String toString() {
        return "od.pashakka.nbustat.ReportInfo{" +
                "category='" + category + '\'' +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", registryDate='" + registryDate + '\'' +
                ", registryURL='" + registryURL + '\'' +
                ", domainListDate='" + domainListDate + '\'' +
                ", domainListURL='" + domainListURL + '\'' +
                ", descriptionDate='" + descriptionDate + '\'' +
                ", descriptionURL='" + descriptionURL + '\'' +
                ", controlsDate='" + controlsDate + '\'' +
                ", controlsURL='" + controlsURL + '\'' +
                ", schemaURL='" + schemaURL + '\'' +
                '}';
    }

    public String getURL(FileType fileType) {
        return switch (fileType) {
            case REGISTRY -> getRegistryURL();
            case DESCRIPTION -> getDescriptionURL();
            case DOMAIN_LIST -> getDomainListURL();
            case CONTROLS -> getControlsURL();
            case SCHEMA -> getSchemaURL();
        };
    }

    public String getDate(FileType fileType) {
        String date = switch (fileType) {
            case REGISTRY -> getRegistryDate();
            case DESCRIPTION -> getDescriptionDate();
            case DOMAIN_LIST -> getDomainListDate();
            case CONTROLS -> getControlsDate();
            default -> null;
        };
        return reformatDate(date);
    }

    private String reformatDate(String date) {
        if (date == null || date.isEmpty()) return null;
        String[] split = date.split("\\.");
        return split[2] + split[1] + split[0];
    }

    public void setChangedRegistry() {
        changedRegistry = true;
    }

    public void setChangedDomainList() {
        changedDomainList = true;
    }

    public void setChangedDescription() {
        changedDescription = true;
    }

    public void setChangedControls() {
        changedControls = true;
    }

    public boolean isChanged() {
        return changedRegistry || changedDomainList || changedDescription || changedControls;
    }

    public String getChanges() {
        StringBuilder sb = new StringBuilder();

        if (changedRegistry) {
            appendChangedStr(sb, code, FileType.REGISTRY, registryDate);
        }
        if (changedDomainList) {
            appendChangedStr(sb, code, FileType.DOMAIN_LIST, domainListDate);
        }
        if (changedDescription) {
            appendChangedStr(sb, code, FileType.DESCRIPTION, descriptionDate);
        }
        if (changedControls) {
            appendChangedStr(sb, code, FileType.CONTROLS, controlsDate);
        }

        return sb.toString();
    }

    private void appendChangedStr(StringBuilder sb, String code, FileType fileType, String date) {
        sb.append("changed -> ")
                .append(code)
                .append("(").append(fileType).append(") ")
                .append(date).append("\n");

    }
}
