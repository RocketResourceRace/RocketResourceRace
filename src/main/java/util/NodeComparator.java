package util;

import java.util.Comparator;

public class NodeComparator implements Comparator<Node> {
    public int compare (Node o1, Node o2){
        return Integer.compare(o1.cost, o2.cost);
    }
    public boolean equals(Node a, Node b){
        return a.cost == b.cost;
    }
}