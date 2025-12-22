package ir.monopoly.server.datastructure;

import ir.monopoly.server.player.Player;

public class WealthBST {
    private Node root;

    private static class Node {
        Player player;
        Node left, right;
        Node(Player player) { this.player = player; }
    }

    public void insert(Player player) {
        root = insertRecursive(root, player);
    }

    private Node insertRecursive(Node current, Player player) {
        if (current == null) return new Node(player);

        if (player.getTotalWealth() < current.player.getTotalWealth()) {
            current.left = insertRecursive(current.left, player);
        } else if (player.getTotalWealth() > current.player.getTotalWealth()) {
            current.right = insertRecursive(current.right, player);
        } else {
            current.player = player;
        }
        return current;
    }

    public void update(Player player) {
        root = deleteRecursive(root, player);
        insert(player);
    }

    private Node deleteRecursive(Node current, Player player) {
        if (current == null) return null;

        if (player.getTotalWealth() < current.player.getTotalWealth()) {
            current.left = deleteRecursive(current.left, player);
        } else if (player.getTotalWealth() > current.player.getTotalWealth()) {
            current.right = deleteRecursive(current.right, player);
        } else {
            if (current.left == null) return current.right;
            if (current.right == null) return current.left;

            current.player = findMax(current.left);
            current.left = deleteRecursive(current.left, current.player);
        }
        return current;
    }

    private Player findMax(Node node) {
        while (node.right != null) node = node.right;
        return node.player;
    }

    public Player[] getSortedPlayers() {
        int count = countNodes(root);
        Player[] result = new Player[count];
        inorder(root, result, new int[]{0});
        return result;
    }

    private int countNodes(Node node) {
        if (node == null) return 0;
        return 1 + countNodes(node.left) + countNodes(node.right);
    }

    private void inorder(Node node, Player[] result, int[] index) {
        if (node != null) {
            inorder(node.left, result, index);
            result[index[0]++] = node.player;
            inorder(node.right, result, index);
        }
    }

    public String getSortedWealthString() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Players List by Wealth (BST) ---\n");
        Player[] sorted = getSortedPlayers();
        for (int i = sorted.length - 1; i >= 0; i--) {
            Player p = sorted[i];
            sb.append(p.getName()).append(": $").append(p.getTotalWealth()).append("\n");
        }
        return sb.toString();
    }
}