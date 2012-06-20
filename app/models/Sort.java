package models;

/**
 * Do not change order of these enums, as their ordinals are used in user view options stored in session
 */
public enum Sort {
    DEFAULT("completed, orderIndex"),
    PRIORITY_DESC("completed, priority desc, dateDue, orderIndex"),
    DUE_DATE_DESC("completed, dateDue desc, priority, orderIndex"),
    DATE_CREATED_DESC("completed, dateCreated desc, priority, orderIndex"),
    LAST_UPDATED_DESC("completed, lastUpdated desc, priority, orderIndex"),
    PRIORITY_ASC("completed, priority asc, dateDue, orderIndex"),
    DUE_DATE_ASC("completed, dateDue asc, priority, orderIndex"),
    DATE_CREATED_ASC("completed, dateCreated asc, priority, orderIndex"),
    LAST_UPDATED_ASC("completed, lastUpdated asc, priority, orderIndex");

    private String sqlSort;
    
    private Sort(String sqlSort) {
        this.sqlSort = sqlSort;
    }
    
    public String getSqlSort() {
        return sqlSort;
    }
}
