package com.amon.flashtsdb.sdt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2019/11/27.
 */
@Service
public class SdtService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * SDT algorithm compress function
     *
     * @param originPoints
     * @param accuracyE
     * @return
     */
    public List<SdtPeriod> sdtCompress(List<Point> originPoints, double accuracyE) {
        List<SdtPeriod> sdtPeriodList = new ArrayList<>();

        if (null != originPoints && originPoints.size() > 0) {
            SdtPoints sdtPoints = new SdtPoints();

            double upGate = -Double.MAX_VALUE;
            double downGate = Double.MAX_VALUE;
            for (int i = 0, size = originPoints.size(); i < size; i++) {
                Point currentPoint = originPoints.get(i);

                if (i == 0) {
                    sdtPoints.setLastPoint(currentPoint);
                    sdtPoints.setBeginPoint(currentPoint);
                } else {

                    double nowUpGate = (currentPoint.getY() - sdtPoints.getBeginPoint().getY() - accuracyE) /
                            (currentPoint.getX() - sdtPoints.getBeginPoint().getX());

                    if (nowUpGate > upGate) {
                        upGate = nowUpGate;
                    }

                    double nowDownGate = (currentPoint.getY() - sdtPoints.getBeginPoint().getY() + accuracyE) /
                            (currentPoint.getX() - sdtPoints.getBeginPoint().getX());

                    if (nowDownGate < downGate) {
                        downGate = nowDownGate;
                    }

                    if (upGate > downGate) {
                        // create new SdtPeriod(one)
                        sdtPeriodList.add(structSdtPeriod(sdtPoints));

                        // update gates
                        upGate = (currentPoint.getY() - sdtPoints.getLastPoint().getY() - accuracyE) /
                                (currentPoint.getX() - sdtPoints.getLastPoint().getX());
                        downGate = (currentPoint.getY() - sdtPoints.getLastPoint().getY() + accuracyE) /
                                (currentPoint.getX() - sdtPoints.getLastPoint().getX());

                        sdtPoints.setBeginPoint(sdtPoints.getLastPoint());

                    }

                    sdtPoints.setLastPoint(currentPoint);

                }

                if (i == size - 1) {
                    // save last period
                    sdtPeriodList.add(structSdtPeriod(sdtPoints));
                }

            }
        }

        return sdtPeriodList;
    }

    /**
     * SDT algorithm uncompress function
     *
     * @param sdtPeriodList
     * @param bgTime
     * @param endTime
     * @param interval
     * @return
     */
    public List<Point> sdtUnCompress(List<SdtPeriod> sdtPeriodList, long bgTime, long endTime, long interval) {

        List<Point> pointList = new ArrayList<>();

        if (null != sdtPeriodList && sdtPeriodList.size() > 0) {

            // recalculate bgTime and endTime
            long globalBgTime = sdtPeriodList.get(0).getBgTime();
            long globalEndTime = sdtPeriodList.get(sdtPeriodList.size() - 1).getEndTime();

            if (bgTime > globalEndTime || endTime < globalBgTime) {
                return pointList;
            }

            if (bgTime < globalBgTime && endTime < globalEndTime) {
                while (bgTime < globalBgTime) {
                    bgTime += interval;
                }
            }

            if (globalBgTime < bgTime && globalEndTime < endTime) {
                long tempEndTime = bgTime;
                while (tempEndTime < globalEndTime) {
                    tempEndTime += interval;
                }
                endTime = tempEndTime - interval;
            }

            if (bgTime < globalBgTime && endTime > globalEndTime) {
                while (bgTime < globalBgTime) {
                    bgTime += interval;
                }
                long tempEndTime = bgTime;
                while (tempEndTime < globalEndTime) {
                    tempEndTime += interval;
                }
                endTime = tempEndTime - interval;
            }

            // final check
            if (bgTime > globalEndTime || endTime < globalBgTime) {
                return pointList;
            }

            long interpolatingTime = bgTime;
            for (SdtPeriod sdtPeriod : sdtPeriodList) {

                long periodBgTime = sdtPeriod.getBgTime();
                double periodBgValue = sdtPeriod.getBgValue();
                long periodEndTime = sdtPeriod.getEndTime();
                double grad = sdtPeriod.getGradient();

                if (interpolatingTime >= periodBgTime && interpolatingTime <= periodEndTime) {
                    while (interpolatingTime <= periodEndTime && interpolatingTime <= endTime) {
                        Point point = new Point();
                        point.setX(interpolatingTime);
                        point.setY(periodBgValue + (interpolatingTime - periodBgTime) * grad);
                        pointList.add(point);
                        interpolatingTime += interval;
                    }
                }

                if (interpolatingTime > endTime) {
                    break;
                }

            }

        }

        return pointList;
    }

    private SdtPeriod structSdtPeriod(SdtPoints sdtPoints) {
        SdtPeriod sdtPeriod = new SdtPeriod();
        sdtPeriod.setBgTime(sdtPoints.getBeginPoint().getX());
        sdtPeriod.setBgValue(sdtPoints.getBeginPoint().getY());
        sdtPeriod.setEndTime(sdtPoints.getLastPoint().getX());
        sdtPeriod.setEndValue(sdtPoints.getLastPoint().getY());
        sdtPeriod.setGradient((sdtPoints.getLastPoint().getY() - sdtPoints.getBeginPoint().getY()) / (sdtPoints.getLastPoint().getX() - sdtPoints.getBeginPoint().getX()));
        return sdtPeriod;
    }

}
