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
     * @param AccuracyE
     * @return
     */
    public List<SdtPeriod> sdtCompress(List<Point> originPoints, double AccuracyE) {
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

                    double nowUpGate = (currentPoint.getY() - sdtPoints.getBeginPoint().getY() - AccuracyE) /
                            (currentPoint.getX() - sdtPoints.getBeginPoint().getX());

                    if (nowUpGate > upGate) {
                        upGate = nowUpGate;
                    }

                    double nowDownGate = (currentPoint.getY() - sdtPoints.getBeginPoint().getY() + AccuracyE) /
                            (currentPoint.getX() - sdtPoints.getBeginPoint().getX());

                    if (nowDownGate < downGate) {
                        downGate = nowDownGate;
                    }

                    if (upGate > downGate) {
                        // create new SdtPeriod(one)
                        sdtPeriodList.add(structSdtPeriod(sdtPoints));

                        // update gates
                        upGate = (currentPoint.getY() - sdtPoints.getLastPoint().getY() - AccuracyE) /
                                (currentPoint.getX() - sdtPoints.getLastPoint().getX());
                        downGate = (currentPoint.getY() - sdtPoints.getLastPoint().getY() + AccuracyE) /
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
