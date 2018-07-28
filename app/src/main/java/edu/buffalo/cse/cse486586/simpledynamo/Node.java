package edu.buffalo.cse.cse486586.simpledynamo;
/**
 * Created by charanya on 4/20/18.
 */

public class Node implements Comparable<Node> {
    public String port_num;
    public String hash_port;
    public Node succesor = null;
    public Node succesor1 = null;
    public Node predecessor = null;
    public Node predecessor1 = null;

    public Node(String port_num, String hash_port) {
        this.port_num = port_num;
        this.hash_port = hash_port;
    }

    public int compareTo(Node node) {
        if ((this.hash_port.compareTo(node.hash_port)) > 1)
            return 1;
        else if ((this.hash_port.compareTo(node.hash_port)) < 1)
            return -1;
        else
            return 0;
    }

    public void setSuccesor(Node node) {
        this.succesor = node;
    }

    public void setSuccesor1(Node node) {
        this.succesor1 = node;
    }

    public void setPredecessor(Node node) {
        this.predecessor = node;
    }

    public void setPredecessor1(Node node) {
        this.predecessor1 = node;
    }

}
