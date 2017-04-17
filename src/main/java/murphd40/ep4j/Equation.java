package murphd40.ep4j;

import javax.swing.text.html.Option;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by David on 17/04/2017.
 */
public class Equation {

    private Node head;

    public static Equation parse(String equation) {
        return new Equation(equation);
    }

    public Number evaluate() {
        return head.evaluate();
    }

    private Equation(String equation) {
        Tokenizer tokenizer = new Tokenizer(equation);

        Builder builder = new Builder(tokenizer.next());

        while (tokenizer.hasNext()) {
            builder.add(tokenizer.next());
        }

        builder.build();
    }

    enum Operator {
        add((x, y) -> x.doubleValue() + y.doubleValue(), 0, "+"),
        subtract((x, y) -> x.doubleValue() - y.doubleValue(), 0, "-"),
        multiply((x, y) -> x.doubleValue() * y.doubleValue(), 1, "*"),
        divide((x, y) -> x.doubleValue() / y.doubleValue(), 1, "/");

        private final BiFunction<Number, Number, Number> function;
        private final int precedence;
        private final String symbol;

        Operator(BiFunction<Number, Number, Number> function, int precedence, String symbol) {
            this.function = function;
            this.precedence = precedence;
            this.symbol = symbol;
        }

        static Operator of(String symbol) {
            for (Operator operator : Operator.values()) {
                if (operator.symbol.equals(symbol)) {
                    return operator;
                }
            }
            return null;
        }

        Number apply(Number lhs, Number rhs) {
            return function.apply(lhs, rhs);
        }

    }

    private static abstract class Node {
        Node parent, left, right;

        abstract Number evaluate();
    }

    private static class ValueNode extends Node {

        private Number value;

        ValueNode(Number value) {
            this.value = value;
        }

        @Override
        Number evaluate() {
            return value;
        }
    }

    private static class OperatorNode extends Node {

        private Operator operator;

        OperatorNode(Operator operator) {
            this.operator = operator;
        }

        @Override
        Number evaluate() {
            return operator.apply(left.evaluate(), right.evaluate());
        }
    }

    private class Tokenizer implements Iterator<String> {

        private static final String VALUE_REGEX = "[\\d.]+";

        private Queue<String> queue;

        public Tokenizer(String equation) {
            queue = new ArrayDeque<>();

            Matcher matcher = Pattern.compile(VALUE_REGEX).matcher(equation);

            int index = 0;
            while (matcher.find()) {
                addIfNotEmpty(equation.substring(index, matcher.start()).trim());
                addIfNotEmpty(equation.substring(matcher.start(), matcher.end()));
                index = matcher.end();
            }
        }

        private void addIfNotEmpty(String toAdd) {
            if (toAdd.isEmpty()) {
                return;
            }
            queue.add(toAdd);
        }

        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }

        @Override
        public String next() {
            return queue.remove();
        }
    }

    private class Builder {

        private Node head, prev;
        private Queue<OperatorNode> operatorNodes = new ArrayDeque<>();

        private Builder(String value) {
            Number number = Double.valueOf(value);
            head = new ValueNode(number);
            prev = head;
        }

        private Builder add(String value) {
            if (prev instanceof ValueNode) {
                Operator operator = Operator.of(value);
                if (operator == null) {
                    throw new IllegalArgumentException(String.format("%s is not a valid operator.", value));
                }
                operator(operator);
            } else {
                value(Double.valueOf(value));
            }

            return this;
        }

        private void build() {
            Equation.this.head = this.head;
        }

        private void value(Number value) {
            ValueNode node = new ValueNode(value);

            prev.right = node;
            node.parent = prev;

            prev = node;
        }

        private void operator(Operator operator) {
            OperatorNode node = new OperatorNode(operator);

            if (head instanceof ValueNode) {
                node.left = head;
                head.parent = node;
                head = node;
            } else {

                Optional<OperatorNode> operatorNodeToReplace = operatorNodes.stream()
                        .filter(n -> n.operator.precedence > operator.precedence).findFirst();

                if (operatorNodeToReplace.isPresent()) {

                    OperatorNode toReplace = operatorNodeToReplace.get();

                    if (head == toReplace) {
                        head = node;
                    } else {
                        toReplace.parent.right = node;
                        node.parent = toReplace.parent;
                    }

                    node.left = toReplace;
                    toReplace.parent = node;

                } else {
                    // no higher-precedence operator replace higher in the tree
                    prev.parent.right = node;
                    node.parent = prev.parent;

                    node.left = prev;
                    prev.parent = node;
                }
            }

            operatorNodes.add(node);
            prev = node;
        }

    }

}
