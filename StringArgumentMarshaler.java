import java.util.*;

public class StringArgumentMarshaler implements ArgumentMarshaler {

    private String stringValue = "";

    public void set(Iterator<String> currentArgument) throws ArgsException {
        try {
            stringValue = currentArgument.next();
        } catch (NoSuchElementException e) {
            throw new ArgsException(ArgsException.ErrorCode.MISSING_STRING);
        }
    }

    public Object get() {
        return stringValue;
    }
}
