package com.cephx.def.service.db;

import com.cephx.def.DBconnection;
import com.cephx.def.model.CriticalPoint;
import com.cephx.def.repository.CriticalPointRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CriticalPointService {
    private static CriticalPointRepository repository = new CriticalPointRepository();
    public static Map<Integer, CriticalPoint> criticalPoints;

    static {
        criticalPoints = new HashMap<>();
        for (CriticalPoint point: repository.findAll()) {
            criticalPoints.put(point.getPointNumber(), point);
        }
    }

    public static boolean isCriticalPoint(int pointNumber) {
        return criticalPoints.containsKey(pointNumber);
    }

    public static List<Integer>  getCriticalPointsNumbers () {
        return  DBconnection.GetDBconnection().getCriticalPointsTotalNumbers();
    }
}
