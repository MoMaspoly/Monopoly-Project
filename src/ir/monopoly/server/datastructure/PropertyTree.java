package ir.monopoly.server.datastructure;

import ir.monopoly.server.property.Property;
import java.util.ArrayList;
import java.util.List;

public class PropertyTree {
    private Node root;

    private static class Node {
        Property property;
        Node left, right;
        Node(Property property) { this.property = property; }
    }

    public void insert(Property property) {
        root = insertRecursive(root, property);
    }

    private Node insertRecursive(Node current, Property property) {
        if (current == null) return new Node(property);
        if (property.getPropertyId() < current.property.getPropertyId())
            current.left = insertRecursive(current.left, property);
        else if (property.getPropertyId() > current.property.getPropertyId())
            current.right = insertRecursive(current.right, property);
        return current;
    }

    public int countColorGroup(String colorGroup) {
        return countRecursive(root, colorGroup);
    }

    private int countRecursive(Node current, String colorGroup) {
        if (current == null) return 0;
        int count = current.property.getColorGroup().equals(colorGroup) ? 1 : 0;
        return count + countRecursive(current.left, colorGroup) + countRecursive(current.right, colorGroup);
    }

    public void forEach(java.util.function.Consumer<Property> action) {
        List<Property> allProperties = new ArrayList<>();
        collectAllProperties(root, allProperties);
        for (Property prop : allProperties) {
            action.accept(prop);
        }
    }

    private void collectAllProperties(Node current, List<Property> properties) {
        if (current != null) {
            collectAllProperties(current.left, properties);
            properties.add(current.property);
            collectAllProperties(current.right, properties);
        }
    }

    public void remove(int propertyId) {
        root = removeRecursive(root, propertyId);
    }

    private Node removeRecursive(Node current, int propertyId) {
        if (current == null) return null;

        if (propertyId < current.property.getPropertyId()) {
            current.left = removeRecursive(current.left, propertyId);
        } else if (propertyId > current.property.getPropertyId()) {
            current.right = removeRecursive(current.right, propertyId);
        } else {
            if (current.left == null) return current.right;
            if (current.right == null) return current.left;

            current.property = findSmallestValue(current.right);
            current.right = removeRecursive(current.right, current.property.getPropertyId());
        }
        return current;
    }

    private Property findSmallestValue(Node root) {
        return root.left == null ? root.property : findSmallestValue(root.left);
    }

    public boolean isEmpty() {
        return root == null;
    }

    public int size() {
        return countSize(root);
    }

    private int countSize(Node node) {
        if (node == null) return 0;
        return 1 + countSize(node.left) + countSize(node.right);
    }
}