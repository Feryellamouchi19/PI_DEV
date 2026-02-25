package services;

import java.time.LocalDateTime;

public class FilterCriteria {
    private String type;            // ex: SOIREE, CAMPING, ...
    private LocalDateTime dateFrom; // début intervalle
    private LocalDateTime dateTo;   // fin intervalle
    private String keyword;         // recherche texte

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getDateFrom() { return dateFrom; }
    public void setDateFrom(LocalDateTime dateFrom) { this.dateFrom = dateFrom; }

    public LocalDateTime getDateTo() { return dateTo; }
    public void setDateTo(LocalDateTime dateTo) { this.dateTo = dateTo; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}