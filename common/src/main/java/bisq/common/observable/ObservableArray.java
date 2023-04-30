/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.common.observable;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

public class ObservableArray<T> implements List<T> {
    private final List<T> collection = new CopyOnWriteArrayList<>();
    // Must be a list, not a set as otherwise if 2 instances of the same component is using it, one would get replaced.
    private final List<Observer<T>> observers = new CopyOnWriteArrayList<>();

    public ObservableArray() {
    }

    public ObservableArray(Collection<T> values) {
        addAll(values);
    }

    private List<Observer<T>> getObservers() {
        return observers;
    }

    public Pin addChangedListener(Runnable handler) {
        ChangeListener<T> changedListener = new ChangeListener<>(handler);
        getObservers().add(changedListener);
        handler.run();
        return () -> getObservers().remove(changedListener);
    }

    public <L> Pin addObservableListMapper(Collection<L> collection, Function<T, L> mapFunction, Consumer<Runnable> executor) {
        ObservableListMapper<T, L> observableListMapper = new ObservableListMapper<>(collection, mapFunction, executor);
        observableListMapper.clear();
        observableListMapper.addAll(this);
        getObservers().add(observableListMapper);
        return () -> getObservers().remove(observableListMapper);
    }

    @Override
    public boolean add(T element) {
        boolean result = collection.add(element);
        if (result) {
            getObservers().forEach(observer -> observer.add(element));
        }
        return result;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> values) {
        boolean result = collection.addAll(values);
        if (result) {
            getObservers().forEach(observer -> observer.addAll(values));
        }
        return result;
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends T> c) {
        boolean result = collection.addAll(index, c);
        if (result) {
            getObservers().forEach(observer -> observer.addAll(c));
        }
        return result;
    }

    @Override
    public boolean remove(Object element) {
        boolean result = collection.remove(element);
        if (result) {
            getObservers().forEach(observer -> observer.remove(element));
        }
        return result;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> values) {
        boolean result = collection.removeAll(values);
        if (result) {
            getObservers().forEach(observer -> observer.removeAll(values));
        }
        return result;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        //todo
        return collection.retainAll(c);
    }

    @Override
    public void clear() {
        collection.clear();
        getObservers().forEach(Observer::clear);
    }


    @Override
    public T set(int index, T element) {
        //todo
        return collection.set(index, element);
    }

    @Override
    public void add(int index, T element) {
        //todo
        collection.add(index, element);
    }

    @Override
    public T remove(int index) {
        //todo
        return collection.remove(index);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return collection.containsAll(c);
    }

    @Override
    public int size() {
        return collection.size();
    }

    @Override
    public boolean isEmpty() {
        return collection.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return collection.contains(o);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return collection.iterator();
    }

    @Override
    public Object[] toArray() {
        return collection.toArray();
    }

    @Override
    public <T1> T1[] toArray(@NotNull T1[] a) {
        return collection.toArray(a);
    }

    @Override
    public T get(int index) {
        return collection.get(index);
    }

    @Override
    public int indexOf(Object o) {
        return collection.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return collection.lastIndexOf(o);
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator() {
        return collection.listIterator();
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator(int index) {
        return collection.listIterator(index);
    }

    @NotNull
    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return collection.subList(fromIndex, toIndex);
    }

    @Override
    public void sort(Comparator<? super T> c) {
        collection.sort(c);
    }
}