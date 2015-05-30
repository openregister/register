package uk.gov.openregister.store.postgresql;

@FunctionalInterface
public interface FunctionThatThrows<T, R> {
    default R andThen(T t) {
        try {
            return acceptThrows(t);
        } catch (RuntimeException e) { throw e; }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    R acceptThrows(T elem) throws Exception;
}
