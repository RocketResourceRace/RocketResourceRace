package util;

import java.util.Comparator;

public class NodeComparator implements Comparator {
        public int compare (Object o1, Object o2){
            Node a = (Node)o1;
            Node b = (Node)o2;
            if (a.cost < b.cost){
                return -1;
            } else if (a.cost > b.cost){
                return 1;
            } else {
                return 0;
            }
        }
        public boolean equals(Node a, Node b){
            return a.cost == b.cost;
        }
    }