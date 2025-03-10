import java.io.*;
import java.util.Stack;

public class CalculatorTest
{
	public static void main(String args[]) {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			try {
				String input = br.readLine();
				if (input.compareTo("q") == 0)
					break;

				command(input);
			} catch (Exception e) {
				System.out.println("입력이 잘못되었습니다. 오류 : " + e.toString());
			}
		}
	}

	private static void command(String input) {
		String postfix = infixToPostfix(input);
		if (postfix.compareTo("ERROR") == 0) {
			System.out.println("ERROR");
			return ;
		}
		String result = calculatePostfix(postfix);
		if (result.compareTo("ERROR") == 0) {
			System.out.println("ERROR");
			return ;
		}
		System.out.println(postfix);
		System.out.println(result);
	}

    /*

	Skeleton code infixToPostfix() by ChatGPT

	static int prec(char ch) {
        switch (ch) {
            case '+':
            case '-':
                return 1;

            case '*':
            case '/':
                return 2;

            case '^':
                return 3;
        }
        return -1;
    }

    static String infixToPostfix(String exp) {
        String result = "";
        Deque<Character> stack = new ArrayDeque<Character>();

        for (int i = 0; i < exp.length(); ++i) {
            char c = exp.charAt(i);

            if (Character.isDigit(c)) {
                while (i < exp.length() && Character.isDigit(exp.charAt(i))) {
                    result += exp.charAt(i++);
                }
                i--;
                result += " ";
            } else if (c == '(') {
                if (i + 1 < exp.length() && exp.charAt(i + 1) == '-') {
                    stack.push('@'); // Represent unary minus with '@'
                    i++;
                } else {
                    stack.push(c);
                }
            } else if (c == ')') {
                while (!stack.isEmpty() && stack.peek() != '(') {
                    result += stack.pop() + " ";
                }
                stack.pop();
            } else {
                while (!stack.isEmpty() && prec(c) <= prec(stack.peek())) {
                    result += stack.pop() + " ";
                }
                stack.push(c);
            }
        }

        while (!stack.isEmpty()) {
            if (stack.peek() == '(') {
                return "Invalid Expression";
            }
            result += stack.pop() + " ";
        }

        return result;
    }

	 */

	private static int precedence(char c) {
		if (c == '+' || c == '-')
			return (1);
		else if (c == '*' || c == '/' || c == '%')
			return (2);
		else if (c == '~')
			return (3);
		else if (c == '^')
			return (4);
		else
			return (-1);
	}

	private static String infixToPostfix(String input) {
		String result = "";
		Stack<Character> stack = new Stack<>();
		boolean isPreviousDigit = false;		// to classify '-' unary or binary
		boolean isPreviousSpace = false;		// to detect "123 123" is ERROR
		/*
			in case of "E ^ - E", the operator precedence and postfix expression can cause ambiguous result
			so case of "E ^ - E" is handled as ERROR

			Example:
			1 / 100 ^ - 3
			will be the postfix expression
			1 100 ^ 3 ~ /
			and this is not the expected expression.
		*/
		boolean isPreviousPower = false;
		long	averageCount = 0;	//for average operator

		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);

			if (Character.isDigit(c)) {				//if number, append to postfix expression
				if (isPreviousSpace && isPreviousDigit)
					return "ERROR";
				while (i < input.length() && Character.isDigit(input.charAt(i))) {
					result += input.charAt(i++);
				}
				i--;
				result += " ";
				isPreviousDigit = true;
				isPreviousSpace = false;
				isPreviousPower = false;
			} else if (c == ' ' || c == '\t') {		//Ignore space character
				isPreviousSpace = true;
			} else if (c == ',') {					//For average operator, push ',' to stack and count when meet ')'
				if (!isPreviousDigit)
					return "ERROR";
				while (!stack.isEmpty() &&
						(stack.peek() != '(') && stack.peek() != ',') {		//in average expression, each expression have to be calculated before next expression
					result += stack.pop() + " ";
				}
				if (stack.size() == 0)
					return "ERROR";
				isPreviousSpace = false;
				isPreviousDigit = false;
				isPreviousPower = false;
				stack.push(c);
			} else if (c == '(') {
				stack.push(c);
				isPreviousDigit = false;
				isPreviousSpace = false;
				isPreviousPower = false;
			} else if (c == ')') {
				if (!isPreviousDigit)
					return "ERROR";
				while (!stack.isEmpty() && stack.peek() != '(') {
					if (stack.peek() == ',') {					//if stack top is ',', that parenthesis was average operator
						stack.pop();
						averageCount++;
					}
					else										//else just parenthesis
						result += stack.pop() + " ";
				}
				if (stack.size() == 0)
					return "ERROR";
				stack.pop();
				if (averageCount != 0)							//if parenthesis was average operator
				{
					result += Long.toString(averageCount + 1) + " avg ";
					averageCount = 0;
				}
				isPreviousSpace = false;
				isPreviousPower = false;
			} else {											//operator
				if (c == '-' && !isPreviousDigit)				//classify unary or binary '-'
				{
					if (isPreviousPower)
						return "ERROR";
					else
						c = '~';
				}
				if (precedence(c) == -1)						// if not allowed character, return "ERROR"
					return "ERROR";
				while (!stack.isEmpty() && precedence(c) <= precedence(stack.peek()) &&
						!(precedence(c) == precedence(stack.peek()) && (c == '~' || c == '^'))) {	//pop if right-associative
					result += stack.pop() + " ";
				}
				stack.push(c);
				isPreviousDigit = false;
				isPreviousSpace = false;
				isPreviousPower = false;
				if (c == '^')
					isPreviousPower = true;
			}
		}

		while (!stack.isEmpty()) {
			if (stack.peek() == '(') {
				return "ERROR";
			}
			result += stack.pop() + " ";
		}
		return result.substring(0, result.length() - 1);
	}

	/*
		Check if the string is number, Not necessary
	*/
	private static boolean isDigitString(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isDigit(s.charAt(i)))
				return false;
		}
		return true;
	}

	/*
		Postfix Calculator
	 */
	private static String calculatePostfix(String postfix) {	//Return type is string to return "ERROR" when ERROR occurs
		Stack<Long> stack = new Stack<>();
		String[] string = postfix.split(" ");			//Postfix is right-formatted, so just split it by ' '

		for (String elem : string) {
			if (isDigitString(elem)) {
				stack.push(Long.parseLong(elem));
			} else if (elem.compareTo("~") == 0) {		// ~ is unary calculator, so implemented differently
				if (stack.size() == 0)
					return "ERROR";
				Long operand = stack.pop();
				operand = -operand;
				stack.push(operand);
			} else if(elem.compareTo("avg") == 0) {		// average operator
				Long count = stack.pop();
				Long sum = (long) 0;
				for (int i = 0; i < count; i++)
					sum += stack.pop();
				stack.push(sum / count);
			} else {									// other operators
				if (stack.size() < 2)
					return "ERROR";
				Long operand2 = stack.pop();
				Long operand1 = stack.pop();
				if (elem.compareTo("+") == 0)
					stack.push(operand1 + operand2);
				else if (elem.compareTo("-") == 0)
					stack.push(operand1 - operand2);
				else if (elem.compareTo("*") == 0)
					stack.push(operand1 * operand2);
				else if (elem.compareTo("/") == 0)
				{
					if (operand2 == 0)
						return "ERROR";
					stack.push(operand1 / operand2);
				}
				else if (elem.compareTo("^") == 0){
					if (operand1 == 0 && operand2 < 0)
						return "ERROR";
					stack.push((long) Math.pow(operand1, operand2));
				}
				else if (elem.compareTo("%") == 0){
					if (operand2 == 0)
						return "ERROR";
					stack.push(operand1 % operand2);
				}
			}
		}
		if (stack.size() != 1)	//if stack.size() != 1, then the postfix was not properly formatted because infix was not properly formatted.
			return "ERROR";
		Long result = stack.pop();
		return (result.toString());
	}
}
