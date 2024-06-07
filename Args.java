import java.text.ParseException;
import java.util.*;

public class Args {

    private String schema;
    private String[] args;
    private boolean valid = true;
    private Set<Character> unexpectedArguments = new TreeSet<Character>();
    private Map<Character, ArgumentMarshaler> booleanArgs = new HashMap<
        Character,
        ArgumentMarshaler
    >();
    private Map<Character, ArgumentMarshaler> stringArgs = new HashMap<
        Character,
        ArgumentMarshaler
    >();
    private Map<Character, ArgumentMarshaler> intArgs = new HashMap<
        Character,
        ArgumentMarshaler
    >();
    private Map<Character, ArgumentMarshaler> marshalers = new HashMap<
        Character,
        ArgumentMarshaler
    >();
    private Set<Character> argsFound = new HashSet<Character>();
    private int currentArgument;
    private char errorArgumentId = '\0';

    enum ErrorCode {
        OK,
        MISSING_STRING,
    }

    private ErrorCode errorCode = ErrorCode.OK;

    public Args(String schema, String[] args) throws ParseException {
        this.schema = schema;
        this.args = args;
        valid = parse();
    }

    private boolean parse() throws ParseException {
        if (schema.length() == 0 && args.length == 0) return true;
        parseSchema();
        parseArguments();
        return valid;
    }

    private boolean parseSchema() throws ParseException {
        for (String element : schema.split(",")) {
            if (element.length() > 0) {
                String trimmedElement = element.trim();
                parseSchemaElement(trimmedElement);
            }
        }
        return true;
    }

    private void parseSchemaElement(String element) throws ParseException {
        char elementId = element.charAt(0);
        String elementTail = element.substring(1);
        validateSchemaElementId(elementId);
        if (isBooleanSchemaElement(elementTail)) parseBooleanSchemaElement(
            elementId
        );
        else if (isStringSchemaElement(elementTail)) parseStringSchemaElement(
            elementId
        );
    }

    private void validateSchemaElementId(char elementId) throws ParseException {
        if (!Character.isLetter(elementId)) {
            throw new ParseException(
                "Bad character:" + elementId + "in Args format: " + schema,
                0
            );
        }
    }

    private void parseStringSchemaElement(char elementId) {
        ArgumentMarshaler m = new StringArgumentMarshaler();
        stringArgs.put(elementId, m);
        marshalers.put(elementId, m);
    }

    private boolean isStringSchemaElement(String elementTail) {
        return elementTail.equals("*");
    }

    private void parseBooleanSchemaElement(char elementId) {
        ArgumentMarshaler m = new BooleanArgumentMarshaler();
        booleanArgs.put(elementId, m);
        marshalers.put(elementId, m);
    }

    private boolean isBooleanSchemaElement(String elementTail) {
        return elementTail.length() == 0;
    }

    private void parseIntegerSchemaElement(char elementId) {
        ArgumentMarshaler m = new IntegerArgumentMarshaler();
        intArgs.put(elementId, m);
        marshalers.put(elementId, m);
    }

    private boolean parseArguments() {
        for (
            currentArgument = 0;
            currentArgument < args.length;
            currentArgument++
        ) {
            String arg = args[currentArgument];
            parseArgument(arg);
        }
        return true;
    }

    private void parseArgument(String arg) {
        if (arg.startsWith("-")) parseElements(arg);
    }

    private void parseElements(String arg) {
        for (int i = 1; i < arg.length(); i++) parseElement(arg.charAt(i));
    }

    private void parseElement(char argChar) {
        if (setArgument(argChar)) argsFound.add(argChar);
        else {
            unexpectedArguments.add(argChar);
            valid = false;
        }
    }

    private boolean setArgument(char argChar) throws ArgsException {
        ArgumentMarshaler m = marshalers.get(argChar);
        try {
            if (m instanceof BooleanArgumentMarshaler) setBooleanArg(m);
            else if (m instanceof StringArgumentMarshaler) setStringArg(m);
            else if (m instanceof IntegerArgumentMarshaler) setIntArg(m);
            else return false;
        } catch (ArgsException e) {
            valid = false;
            errorArgumentId = argChar;
            throw e;
        }
        return true;
    }

    private void setStringArg(ArgumentMarshaler m) throws ArgsException {
        currentArgument++;
        try {
            m.set(args[currentArgument]);
        } catch (ArrayIndexOutOfBoundsException e) {
            errorCode = ErrorCode.MISSING_STRING;
            throw new ArgsException();
        }
    }

    private void setBooleanArg(ArgumentMarshaler m) {
        try {
            m.set("true");
        } catch (ArgsException e) {}
    }

    private void setIntArg(ArgumentMarshaler m) throws ArgsException {
        currentArgument++;
        String parameter = null;
        try {
            parameter = args[currentArgument];
            m.set(parameter);
        } catch (ArrayIndexOutOfBoundsException e) {
            errorCode = ErrorCode.MISSING_INTEGER;
            throw new ArgsException();
        } catch (ArgsException e) {
            errorParameter = parameter;
            errorCode = ErrorCode.INVALID_INTEGER;
            throw e;
        }
    }

    public int cardinality() {
        return argsFound.size();
    }

    public String usage() {
        if (schema.length() > 0) return "-[" + schema + "]";
        else return "";
    }

    public String errorMessage() throws Exception {
        if (unexpectedArguments.size() > 0) {
            return unexpectedArgumentMessage();
        } else switch (errorCode) {
            case MISSING_STRING:
                return String.format(
                    "Could not find string parameter for -%c.",
                    errorArgumentId
                );
            case OK:
                throw new Exception("TILT: Should not get here.");
        }
        return "";
    }

    private String unexpectedArgumentMessage() {
        StringBuffer message = new StringBuffer("Argument(s) -");
        for (char c : unexpectedArguments) {
            message.append(c);
        }
        message.append(" unexpected.");
        return message.toString();
    }

    public boolean getBoolean(char arg) {
        Args.ArgumentMarshaler am = booleanArgs.get(arg);
        return am != null && (Boolean) am.get();
    }

    public String getString(char arg) {
        Args.ArgumentMarshaler am = stringArgs.get(arg);
        return am == null ? "" : (String) am.get();
    }

    public int getInt(char arg) {
        Args.ArgumentMarshaler am = intArgs.get(arg);
        return am == null ? 0 : (Integer) am.get();
    }

    public boolean has(char arg) {
        return argsFound.contains(arg);
    }

    public boolean isValid() {
        return valid;
    }

    private abstract class ArgumentMarshaler {

        public abstract void set(String s);

        public abstract Object get();
    }

    private class BooleanArgumentMarshaler extends ArgumentMarshaler {

        private boolean booleanValue = false;

        public void set(String s) {
            booleanValue = true;
        }

        public Object get() {
            return booleanValue;
        }
    }

    private class StringArgumentMarshaler extends ArgumentMarshaler {

        private String stringValue;

        public void set(String s) {
            stringValue = s;
        }

        public Object get() {
            return stringValue;
        }
    }

    private class IntegerArgumentMarshaler extends ArgumentMarshaler {

        private int intValue = 0;

        public void set(String s) throws ArgsException {
            try {
                intValue = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new ArgsException();
            }
        }

        public Object get() {
            return intValue;
        }
    }

    private class ArgsException extends Exception {}
}
