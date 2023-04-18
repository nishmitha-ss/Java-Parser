package parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Parser {
    private class Line_Memory{
        short line_number;
        String line;
    }

    private final ArrayList<Line_Memory> program;
    private final HashMap<String, Float> variables;
    private final Stack<Float> stack;
    private final Stack<Integer> gosub;
    private int curr_line;

    private String to_upper(String str)
    {
        boolean print = false;
        StringBuilder builder = new StringBuilder();

        for(int i = 0;i < str.length();i++){
            if(str.charAt(i) == '\"')
                print = !print;

            if(!print)
                builder.append(Character.toUpperCase(str.charAt(i)));
            else
                builder.append(str.charAt(i));
        }
        return builder.toString();
    }

    Parser(String filename) throws FileNotFoundException
    {
        File fin = new File(filename);
        Scanner sc = new Scanner(fin);
        program = new ArrayList<>();
        variables = new HashMap<>();
        stack = new Stack<>();
        gosub = new Stack<>();
        curr_line = 0;

        while (sc.hasNextLine()){
            Line_Memory temp = new Line_Memory();
            temp.line_number = sc.nextShort();

            sc.skip(" ");

            temp.line = to_upper(sc.nextLine());
            program.add(temp);
        }
    }

    private String getnextstring(String str, int[] start)
    {
        StringBuilder newstr = new StringBuilder();
        try{
            while (str.charAt(start[0]) == ' ' || str.charAt(start[0]) == '\t')
                start[0]++;

            String ch = String.valueOf(str.charAt(start[0]));
            if("*/+-=!,<>\"".contains(ch)) {
                start[0]++;
                return ch;
            }

            for (;str.charAt(start[0]) != ' ' && str.charAt(start[0]) != '\t'; start[0]++)
            {
                ch = String.valueOf(str.charAt(start[0]));
                if("*/+-=!,<>\"".contains(ch))
                    break;
                newstr.append(str.charAt(start[0]));
            }
        }
        catch (IndexOutOfBoundsException ignored){}

        return newstr.toString();
    }

    private Float getValue(String str)
    {
        try{
            return Float.parseFloat(str);
        }
        catch (NumberFormatException e){
            return variables.get(str);
        }
    }

    private void print_float(Float num)
    {
        if(num - num.intValue() > 0)
            System.out.print(num);
        else
            System.out.print(num.intValue());
    }

    private int get_precedence(String str)
    {
        if(str.equals("*") || str.equals("/"))
            return 2;
        if(str.equals("+") || str.equals("-"))
            return 1;
        return -1;
    }

    private Float evaluate_expression(String str)
    {
        int[] curr = {0};
        String next = "";

        Stack<Float> values = new Stack<>();
        Stack<String> operators = new Stack<>();

        while(!(next = getnextstring(str, curr)).equals(""))
        {
            if("*/+-".contains(next))
            {
                while(!operators.isEmpty() && get_precedence(operators.peek()) >= get_precedence(next))
                {
                    Float num2 = values.pop();
                    Float num1 = values.pop();
                    switch (operators.pop()) {
                        case "*" -> values.push(num1 * num2);
                        case "/" -> values.push(num1 / num2);
                        case "+" -> values.push(num1 + num2);
                        case "-" -> values.push(num1 - num2);
                    }
                }
                operators.push(next);
            }
            else
            {
                values.push(getValue(next));
            }
        }

        while(!operators.isEmpty())
        {
            Float num2 = values.pop();
            Float num1 = values.pop();
            switch (operators.pop()) {
                case "*" -> values.push(num1 * num2);
                case "/" -> values.push(num1 / num2);
                case "+" -> values.push(num1 + num2);
                case "-" -> values.push(num1 - num2);
            }
        }
        return values.pop();
    }


    public boolean execute(String arg) throws NullPointerException
    {
        int[] curr = {0};
        String cmd = getnextstring(arg, curr), next = "";

        Scanner sc = new Scanner(System.in);

        switch (cmd) {
            case "LET" -> {
                String var = getnextstring(arg, curr);
                if(variables.containsKey(var))
                    variables.put(var, evaluate_expression(arg.substring(curr[0])));
                else
                    throw new NullPointerException();
            }

            case "PRINT" -> {
                int prev = curr[0];
                next = getnextstring(arg, curr);
                if(next.equals("\""))
                {
                    for (;arg.charAt(curr[0]) != '\"';curr[0]++)
                        System.out.print(arg.charAt(curr[0]));
                    curr[0]++;
                    next = getnextstring(arg, curr);
                    if(next.equals(","))
                    {
                        print_float(evaluate_expression(arg.substring(curr[0])));
                    }
                }
                else {
                    curr[0] = prev;
                    print_float(evaluate_expression(arg.substring(curr[0])));
                }
            }

            case "PRINTLN" -> {
                int prev = curr[0];
                next = getnextstring(arg, curr);
                if (next.equals("\""))
                {
                    for (; arg.charAt(curr[0]) != '\"'; curr[0]++)
                        System.out.print(arg.charAt(curr[0]));
                    curr[0]++;
                    next = getnextstring(arg, curr);
                    if(next.equals(","))
                    {
                        print_float(evaluate_expression(arg.substring(curr[0])));
                    }
                }
                else{
                    curr[0] = prev;
                    print_float(evaluate_expression(arg.substring(curr[0])));
                }
                System.out.println();
            }

            case "INTEGER" -> {

                while(!(next = getnextstring(arg, curr)).equals("")){
                    variables.put(next, 0f);
                    curr[0]++;
                }
            }

            case "INPUT" -> {
                String input = sc.nextLine();
                int[] index = {0};
                ArrayList<Float> values = new ArrayList<>();

                try{
                    while(!(next = getnextstring(input, index)).equals("")) {
                        values.add(Float.valueOf(next));
                    }
                }
                catch (NumberFormatException e){
                    System.out.println("Line " + program.get(curr_line).line_number + " missing input value");
                    return false;
                }

                int required = 0;
                while(!(next = getnextstring(arg, curr)).equals(""))
                {
                    if(required < values.size())
                        variables.put(next, values.get(required));
                    else {
                        required--;
                        break;
                    }

                    curr[0]++;
                    required++;
                }

                if (required != values.size())
                {
                    System.out.println("Line " + program.get(curr_line).line_number + " missing input value");
                    return false;
                }
            }

            case "IF" -> {
                StringBuilder builder = new StringBuilder(getnextstring(arg, curr));

                while (!"<!=>".contains(next = getnextstring(arg, curr))){
                    builder.append(next);
                }
                Float var1 = evaluate_expression(builder.toString());

                String cmp = next;
                builder.setLength(0);
                builder.append(getnextstring(arg, curr));

                while(!(next = getnextstring(arg, curr)).equals("THEN")){
                    builder.append(next);
                }
                Float var2 = evaluate_expression(builder.toString());
                boolean result = false;

                switch (cmp){
                    case "<" -> result = var1 < var2;
                    case ">" -> result = var1 > var2;
                    case "=" -> result = Objects.equals(var1, var2);
                    case "!" -> result = !Objects.equals(var1, var2);
                }

                if(result){
                    execute(arg.substring(curr[0]).trim());
                }
            }

            case "GOTO" -> {
                short line = Short.parseShort(getnextstring(arg, curr));
                int i = 0;
                for(; i < program.size(); i++){
                    if(program.get(i).line_number == line){
                        curr_line = i - 1;
                        break;
                    }
                }
                if (i == program.size())
                {
                    System.out.println("Line number not found - " + line);
                    return false;
                }
            }

            case "GOSUB" -> {
                short line = Short.parseShort(getnextstring(arg, curr));
                int i = 0;
                gosub.push(curr_line + 1);
                for(; i < program.size(); i++) {
                    if (program.get(i).line_number == line) {
                        curr_line = i - 1;
                        break;
                    }
                }

                if (i == program.size())
                {
                    System.out.println("Line number not found - " + line);
                    return false;
                }
            }

            case "RET" -> {
                curr_line = gosub.pop() - 1;
            }

            case "PUSH" -> {
                stack.push(evaluate_expression(arg.substring(curr[0])));
            }

            case "POP" -> {
                next = getnextstring(arg, curr);
                variables.put(next, stack.pop());
            }

            case "END" -> {
                End();
            }

            default -> System.out.println("\nCould not parse statement!\n");
        }

        return true;
    }

    public void run_program()
    {
        try {
            for (curr_line = 0; curr_line < program.size(); curr_line++) {
                if (!execute(program.get(curr_line).line))
                    break;
            }
            if (curr_line == program.size())
                System.out.println("END Statement is missing!");
        }
        catch (NullPointerException e){
            System.out.println("Line " + program.get(curr_line).line_number + " undeclared variables");
        }
    }

    public void End(){
        System.exit(0);
    }

    public static void main(String[] args) {

        try {
            Parser parse = new Parser("input.txt");

            parse.run_program();
        }
        catch (FileNotFoundException e){
            System.out.println("Could not open file!!!");
        }
    }
}