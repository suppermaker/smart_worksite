package com.xd.smartworksite.datasource.infra;

public final class SqlSummary {

    private SqlSummary() {
    }

    public static String summarize(String sql) {
        if (sql == null) {
            return null;
        }
        String compact = sql.replaceAll("\\s+", " ").trim();
        return compact.length() <= 300 ? compact : compact.substring(0, 300);
    }
}
