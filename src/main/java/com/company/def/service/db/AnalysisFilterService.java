package com.cephx.def.service.db;

import com.cephx.def.DBconnection;

public class AnalysisFilterService {
    private static DBconnection connection = DBconnection.GetDBconnection();

    public static void removeAnalysisFilter(long filterId) throws Exception {
        String sql = "DELETE FROM a_groups WHERE id=" + filterId;
        connection.execStatement(sql);
    }

    public static void addAnalysisFilter(long doctorId, String filterName, String content) throws Exception {
        String sql = "INSERT INTO a_groups(owner_num,Name,content,tStamp) VALUES("
                + doctorId + ",'" + filterName + "','" + content + "',now())";
        connection.execStatement(sql);
    }

    public static void copyAnalysisFiltersToDoctor(long fromDoctor, long toDoctor) throws Exception {
        String sql = "INSERT INTO a_groups (owner_num, Name, content, tStamp) " +
                "SELECT " + toDoctor + ", Name, content, tStamp FROM a_groups WHERE owner_num =" + fromDoctor ;
        connection.execStatement(sql);
    }


}
