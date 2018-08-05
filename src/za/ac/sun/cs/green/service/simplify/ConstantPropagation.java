package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;

public class ConstantPropagation extends BasicService {
	/**
	 * Number of times the slicer has been invoked.
	 */
	private int invocations = 0;

	public ConstantPropagation(Green solver) {
		super(solver);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = propagate(instance.getFullExpression(), map);
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
		}
		return result;
	}

	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
	}

	public Expression propagate(Expression expression, Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Constant Propagation: " + expression);
			invocations++;
			PropogationVisitor propogationVisitor = new PropogationVisitor();
			expression.accept(propogationVisitor);
			expression = propogationVisitor.getExpression();
			log.log(Level.FINEST, "After Constant Propagation: " + expression);
			return expression;
		} catch (VisitorException x) {
			log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", x);
		}
		return null;
	}

	private static class PropogationVisitor extends Visitor {
        private Stack<Expression> stack;
        private HashMap<IntVariable, IntConstant> variables;

		public PropogationVisitor() {
            stack = new Stack<Expression>();
            variables = new HashMap<IntVariable, IntConstant>();
		}

		public Expression getExpression() {
            Expression finalExp = stack.pop();
            System.out.println("Final expression is " + finalExp);
			return finalExp;
		}

		@Override
		public void postVisit(IntConstant constant) {
            System.out.println("Pushing constant to stack: " + constant);
			stack.push(constant);
		}

		@Override
		public void postVisit(IntVariable variable) {
            if(variables.containsKey(variable)) {
                System.out.println("Pushing constant to stack (propagated). " + variable + " = " + variables.get(variable));
                stack.push(variables.get(variable));
            } else {
                System.out.println("Pushing variable to stack: " + variable + " doesn't have a value.");
                stack.push(variable);
            }
		}
    
        @Override
		public void postVisit(Operation operation) throws VisitorException {
			Operation.Operator op = operation.getOperator();
			Operation.Operator nop = null;
			switch (op) {
			case EQ:
				nop = Operation.Operator.EQ;
				break;
			case NE:
				nop = Operation.Operator.NE;
				break;
			case LT:
				nop = Operation.Operator.GT;
				break;
			case LE:
				nop = Operation.Operator.GE;
				break;
			case GT:
				nop = Operation.Operator.LT;
				break;
			case GE:
				nop = Operation.Operator.LE;
				break;
			default:
				break;
            }
            if (nop == Operation.Operator.EQ) {
                System.out.println("Popping stack");
                Expression r = stack.pop();
                System.out.println("Popping stack");
                Expression l = stack.pop();
                
                if (l instanceof IntVariable && r instanceof IntConstant) {
                    System.out.println("Constant assignment - Map: " + l + " with value " + r);
                    variables.put((IntVariable) l, (IntConstant) r);
                    System.out.println("Pushing EQ with constantable " + l + "==" + variables.get((IntVariable)l));
                    stack.push(new Operation(nop, l, variables.get((IntVariable)l)));
                } else {
                    System.out.println("Pushing non-simple EQ: " + l + "==" + r);
                    stack.push(new Operation(nop, l, r));
                }
            } else if (nop != null) {
                System.out.println("Popping stack (area 1)");
                Expression r = stack.pop();
                System.out.println("Popping stack (area 1)");
                Expression l = stack.pop();
                
				if ((r instanceof IntVariable) && (l instanceof IntVariable) && (((IntVariable) r).getName().compareTo(((IntVariable) l).getName()) < 0)) {
                    Operation newOp = new Operation(nop, l, r);
                    System.out.println("Pushing operation to stack (area 1): " + newOp);
					stack.push(newOp);
				} else if ((r instanceof IntVariable) && (l instanceof IntConstant)) {
                    Operation newOp = new Operation(nop, l, r);
                    System.out.println("Pushing operation to stack (area 1): " + newOp);
					stack.push(newOp);
				} else {
                    System.out.println("Pushing operation to stack (area 1): " + operation);
					stack.push(operation);
				}
			} else if (op.getArity() == 2) {
                System.out.println("Popping stack (area 2)");
                Expression r = stack.pop();
                System.out.println("Popping stack (area 2)");
                Expression l = stack.pop();
                
                Operation newOp = new Operation(op, l, r);
                System.out.println("Pushing operation to stack (area 2): " + newOp);
				stack.push(newOp);
			} else {
				for (int i = op.getArity(); i > 0; i--) {
                    System.out.println("Popping stack (area 3)");
					stack.pop();
                }
                System.out.println("Pushing operation to stack (area 3): " + operation);
				stack.push(operation);
			}
		}

	}

}