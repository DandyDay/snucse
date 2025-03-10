import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BigInteger
{
    private byte[] num;
    private int len;
    private int sign;
    public static final String QUIT_COMMAND = "quit";
    public static final String MSG_INVALID_INPUT = "Wrong Input";

    // implement this
    public static final Pattern EXPRESSION_PATTERN = Pattern.compile("");

    public BigInteger()
    {
        this.num = new byte[201];
        this.len = 0;
        this.sign = 1;
    }

    public BigInteger(String s)
    {
        this.num = new byte[201];
        this.len = s.length();
        this.sign = 1;
        int i = 0;

        if (s.charAt(0) == '+' || s.charAt(0) == '-')
        {
            this.sign = (byte)(44 - s.charAt(0));
            i++;
            this.len--;
        }

        for (; i < s.length(); i++)
        {
            this.num[s.length() - i - 1] = (byte)(s.charAt(i) - '0');
        }
    }

    public BigInteger(BigInteger big)
    {
        this.num = new byte[201];
        this.len = big.len;
        this.sign = big.sign;
        for (int i = 0; i < this.len; i++)
        {
            this.num[i] = big.num[i];
        }
    }

    public BigInteger absAdd(BigInteger big)
    {
        BigInteger result = new BigInteger();
        int carry = 0;
        int idx = 0;
        int calculated = 0;
        while (idx <= big.len || idx <= this.len)
        {
            calculated = carry + this.num[idx] + big.num[idx];
            if (calculated >= 10)
            {
                carry = 1;
                result.num[idx] = (byte)(calculated - 10);
            }
            else
            {
                carry = 0;
                result.num[idx] = (byte)calculated;
            }
            idx++;
        }
        result.len = idx;
        while (result.num[result.len - 1] == 0 && result.len > 1)
            result.len--;
        return result;
    }

    public BigInteger absSubtract(BigInteger big)
    {
        BigInteger tmp = new BigInteger(this);
        BigInteger result = new BigInteger();
        for (int i = 0; i < this.len; i++)
        {
            if (tmp.num[i] - big.num[i] < 0)
            {
                tmp.num[i + 1]--;
                result.num[i] = (byte) (tmp.num[i] - big.num[i] + 10);
            }
            else
                result.num[i] = (byte) (tmp.num[i] - big.num[i]);
        }
        result.len = this.len;
        while (result.num[result.len - 1] == 0)
            result.len--;
        return result;
    }

    public BigInteger add(BigInteger big)
    {
        BigInteger result = new BigInteger();
        if (this.sign == big.sign)
        {
            result = this.absAdd(big);
            result.sign = this.sign;
        }
        else
        {
            if (this.compareAbsTo(big) > 0)
            {
                result = this.absSubtract(big);
                result.sign = this.sign;
            }
            else if (this.compareAbsTo(big) < 0)
            {
                result = big.absSubtract(this);
                result.sign = big.sign;
            }
            else
            {
                result = new BigInteger("0");
            }
        }
        return result;
    }

    public BigInteger subtract(BigInteger big)
    {
        BigInteger result = new BigInteger();
        if (this.sign == big.sign)
        {
            if (this.compareAbsTo(big) > 0)
            {
                result = this.absSubtract(big);
                result.sign = this.sign;
            }
            else if (this.compareAbsTo(big) < 0)
            {
                result = big.absSubtract(this);
                result.sign = this.sign * -1;
            }
            else
                result = new BigInteger("0");
        }
        else
        {
            result = this.absAdd(big);
            result.sign = this.sign;
        }
        return result;
    }

    public BigInteger multiplyByDigit(int digit)
    {
        BigInteger result = new BigInteger("0");
        for (int i = 0; i < digit; i++)
        {
            result = result.add(this);
        }
        return result;
    }

    public BigInteger multiply(BigInteger big)
    {
        BigInteger result = new BigInteger("0");
        BigInteger tmp = new BigInteger();
        for (int i = 0; i < big.len; i++)
        {
            tmp = this.multiplyByDigit(big.num[i]).multiplyTen(i);
            result = result.add(tmp);
        }
        result.sign = this.sign == big.sign ? 1 : -1;
        return result;
    }

    public BigInteger multiplyTen(int exponent)
    {
        for (int i = this.len - 1; i >= 0; i--)
        {
            this.num[i + exponent] = this.num[i];
        }
        for (int i = 0; i < exponent; i++)
            this.num[i] = 0;
        this.len += exponent;
        return this;
    }

    @Override
    public String toString()
    {
        String str = new String();
        if (this.sign == -1)
            str += '-';
        for (int i = this.len - 1; i >= 0; i--)
        {
            str += this.num[i];
        }
        return (str);
    }

    public int compareAbsTo(BigInteger big)
    {
        if (this.len > big.len)
            return (10);
        else if (this.len < big.len)
            return (-10);
        else
        {
            for (int i = this.len - 1; i >= 0; i--)
            {
                if (this.num[i] != big.num[i])
                    return (this.num[i] - big.num[i]);
            }
            return (0);
        }
    }

    static BigInteger evaluate(String input) throws IllegalArgumentException
    {
        // implement here
        // parse input
        // using regex is allowed
        input = input.replaceAll("\\s", "");
        String regex = "([+-]*[0-9]+)([+*-])([+-]*[0-9]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        BigInteger num1 = new BigInteger();
        BigInteger num2 = new BigInteger();
        String op = new String();

        while (matcher.find()) {
            num1 = new BigInteger(matcher.group(1));
            op = matcher.group(2);
            num2 = new BigInteger(matcher.group(3));
        }

        BigInteger result = new BigInteger();
        if (op.compareTo("+") == 0)
        {
            result = num1.add(num2);
        }
        else if (op.compareTo("-") == 0)
        {
            result = num1.subtract(num2);
        }
        else if (op.compareTo("*") == 0)
        {
            result = num1.multiply(num2);
        }
        return result;
    }

    public static void main(String[] args) throws Exception
    {
        try (InputStreamReader isr = new InputStreamReader(System.in))
        {
            try (BufferedReader reader = new BufferedReader(isr))
            {
                boolean done = false;
                while (!done)
                {
                    String input = reader.readLine();

                    try
                    {
                        done = processInput(input);
                    }
                    catch (IllegalArgumentException e)
                    {
                        System.err.println(MSG_INVALID_INPUT);
                    }
                }
            }
        }
    }

    static boolean processInput(String input) throws IllegalArgumentException
    {
        boolean quit = isQuitCmd(input);

        if (quit)
        {
            return true;
        }
        else
        {
            BigInteger result = evaluate(input);
            System.out.println(result.toString());
            return false;
        }
    }

    static boolean isQuitCmd(String input)
    {
        return input.equalsIgnoreCase(QUIT_COMMAND);
    }
}
