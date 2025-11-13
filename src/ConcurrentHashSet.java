import java.util.Iterator;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer; // <-- Make sure to add this import

public class ConcurrentHashSet<T> implements Iterable<T> {

    private final Set<T> concurrentSet;

    public ConcurrentHashSet() {
        concurrentSet = Collections.newSetFromMap(
                new ConcurrentHashMap<>()
        );
    }

    public boolean add(T element) {
        return concurrentSet.add(element);
    }

    public int size() {
        return concurrentSet.size();
    }

    public void forEach(Consumer<? super T> action) {
        concurrentSet.forEach(action);
    }

    @Override
    public Iterator<T> iterator() {
        return concurrentSet.iterator();
    }
}