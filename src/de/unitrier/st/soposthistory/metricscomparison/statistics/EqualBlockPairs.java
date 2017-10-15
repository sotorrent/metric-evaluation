package de.unitrier.st.soposthistory.metricscomparison.statistics;

import java.util.ArrayList;
import java.util.List;

class EqualBlockPairs {

    String content;
    List<Integer> localIdsLeft = new ArrayList<>();
    List<Integer> localIdsRight = new ArrayList<>();

    public EqualBlockPairs(String content){
        this.content = content;
    }
}
