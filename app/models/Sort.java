package models;

/**
 * Do not change order of these enums, as their ordinals are used in user view options stored in session
 */
public enum Sort {
    DEFAULT("todo.completed, todo.orderIndex"),
    PRIORITY_DESC("todo.completed, todo.priority desc, todo.dateDue, todo.orderIndex"),
    DUE_DATE_DESC("todo.completed, todo.dateDue desc, todo.priority, todo.orderIndex"),
    DATE_CREATED_DESC("todo.completed, todo.dateCreated desc, todo.priority, todo.orderIndex"),
    LAST_UPDATED_DESC("todo.completed, todo.lastUpdated desc, todo.priority, todo.orderIndex"),
    PRIORITY_ASC("todo.completed, todo.priority asc, todo.dateDue, todo.orderIndex"),
    DUE_DATE_ASC("todo.completed, todo.dateDue asc, todo.priority, todo.orderIndex"),
    DATE_CREATED_ASC("todo.completed, todo.dateCreated asc, todo.priority, todo.orderIndex"),
    LAST_UPDATED_ASC("todo.completed, todo.lastUpdated asc, todo.priority, todo.orderIndex");

    private String sqlSort;
    
    private Sort(String sqlSort) {
        this.sqlSort = sqlSort;
    }
    
    public String getSqlSort() {
        return sqlSort;
    }
}
