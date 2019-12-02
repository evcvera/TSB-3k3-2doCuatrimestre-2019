package soporte;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public class TSBHashtableDA<K, V> implements Map<K, V>, Cloneable, Serializable {


    private final static int MAX_SIZE = Integer.MAX_VALUE;


    private Entry<K, V> table[];

    /*
    0: Abierta
    1: Cerrada
    2: Tumba
    * */
    private int states[];

    // el tamaño inicial de la tabla (tamaño con el que fue creada)...
    private int initial_capacity;

    // la cantidad de objetos que contiene la tabla...
    private int count;

    // el factor de carga para calcular si hace falta un rehashing...
    private float load_factor;

    // conteo de operaciones de cambio de tamaño (fail-fast iterator).
    protected transient int modCount;

    /**
     * Crea una tabla vacía, con la capacidad inicial igual a 20 y con factor
     * de carga igual a 0.8f.
     */
    public TSBHashtableDA() {
        this(20, 0.8f);
    }

    /**
     * Crea una tabla vacía, con la capacidad inicial indicada y con factor
     * de carga igual a 0.8f.
     * @param initial_capacity la capacidad inicial de la tabla.
     */
    public TSBHashtableDA(int initial_capacity) {
        this(initial_capacity, 0.8f);
    }

    /**
     * Crea una tabla vacía, con la capacidad inicial indicada y con el factor
     * de carga indicado. Si la capacidad inicial indicada por initial_capacity
     * es menor o igual a 0, la tabla será creada de tamaño 11. Si el factor de
     * carga indicado es negativo o cero, se ajustará a 0.8f.
     * @param initial_capacity la capacidad inicial de la tabla.
     * @param load_factor el factor de carga de la tabla.
     */
    public TSBHashtableDA(int initial_capacity, float load_factor) {
        if (load_factor <= 0) {
            load_factor = 0.8f;
        }
        if (initial_capacity <= 0) {
            initial_capacity = 20;
        } else {
            if (initial_capacity > TSBHashtableDA.MAX_SIZE) {
                initial_capacity = TSBHashtableDA.MAX_SIZE;
            } else {
                initial_capacity = this.siguientePrimo(initial_capacity);
            }
        }

        this.table = new Entry[initial_capacity];


        states = new int[initial_capacity];


        for (int i = 0; i < states.length; i++) {
            states[i] = 0;
        }

        this.initial_capacity = initial_capacity;
        this.load_factor = load_factor;
        this.count = 0;
        this.modCount = 0;
    }

    /**
     * Crea una tabla a partir del contenido del Map especificado.
     * @param t el Map a partir del cual se creará la tabla.
     */

    public TSBHashtableDA(Map<? extends K, ? extends V> t) {
        this(20, 0.8f);
        this.putAll(t);
    }

    /**
     * Retorna la cantidad de elementos contenidos en la tabla.
     * @return la cantidad de elementos de la tabla.
     */
    @Override
    public int size() {
        return this.count;
    }

    /**
     * Determina si la tabla está vacía (no contiene ningún elemento).
     * @return true si la tabla está vacía.
     */
    @Override
    public boolean isEmpty() {
        return (this.count == 0);
    }

    /**
     * Determina si la clave key está en la tabla.
     * @param key la clave a verificar.
     * @return true si la clave está en la tabla.
     * @throws NullPointerException si la clave es null.
     */
    @Override
    public boolean containsKey(Object key) {
        return (this.get((K) key) != null);
    }

    /**
     * Determina si alguna clave de la tabla está asociada al objeto value que
     * entra como parámetro. Equivale a contains().
     * @param value el objeto a buscar en la tabla.
     * @return true si alguna clave está asociada efectivamente a ese value.
     */
    @Override
    public boolean containsValue(Object value) {
        return this.contains(value);
    }

    /**
     * Retorna el objeto al cual está asociada la clave key en la tabla, o null
     * si la tabla no contiene ningún objeto asociado a esa clave.
     * @param key la clave que será buscada en la tabla.
     * @return el objeto asociado a la clave especificada (si existe la clave) o
     *         null (si no existe la clave en esta tabla).
     * @throws NullPointerException si key es null.
     * @throws ClassCastException si la clase de key no es compatible con la
     *         tabla.
     */
    @Override
    public V get(Object key) {
        if (key == null)
            throw new NullPointerException("get(): parámetro null");

        int ih = this.h((K) key);
        int ic = ih;
        int j = 1;
        V valueReturn = null;


        while (this.states[ic] != 0) {

            if (this.states[ic] == 1) {
                Entry<K, V> entry = this.table[ic];


                if (key.equals(entry.getKey())) {
                    valueReturn = entry.getValue();
                    return valueReturn;
                }
            }
            ic += j * j;
            j++;
            if (ic >= this.table.length) {
                ic %= this.table.length;
            }
        }
        return valueReturn;
    }

    /**
     * Asocia el valor (value) especificado, con la clave (key) especificada en
     * esta tabla. Si la tabla contenía previamente un valor asociado para la
     * clave, entonces el valor anterior es reemplazado por el nuevo (y en este
     * caso el tamaño de la tabla no cambia).
     * @param key la clave del objeto que se quiere agregar a la tabla.
     * @param value el objeto que se quiere agregar a la tabla.
     * @return el objeto anteriormente asociado a la clave si la clave ya
     *         estaba asociada con alguno, o null si la clave no estaba antes
     *         asociada a ningún objeto.
     * @throws NullPointerException si key es null o value es null.
     */
    @Override
    public V put(K key, V value) {
        if (key == null || value == null)
            throw new NullPointerException("put(): parámetro null");

        int ih = this.h(key);
        int ic = ih;
        int first_tombstone = -1;
        int j = 1;
        V old = null;


        while (this.states[ic] != 0) {


            if (this.states[ic] == 1) {
                Entry<K, V> entry = this.table[ic];

                if (key.equals(entry.getKey())) {
                    old = entry.getValue();
                    entry.setValue(value);
                    this.count++;
                    this.modCount++;

                    return old;
                }
            }


            if (this.states[ic] == 2 && first_tombstone < 0) first_tombstone = ic;


            ic += j * j;
            j++;
            if (ic >= this.table.length) {
                ic %= this.table.length;
            }
        }

        if (first_tombstone >= 0) ic = first_tombstone;


        this.table[ic] = new Entry<K, V>(key, value);
        this.states[ic] = 1;


        this.count++;
        this.modCount++;


        float fc = (float) count / (float) this.table.length;
        if (fc >= this.load_factor)
            this.rehash();

        return old;
    }

    /**
     * Elimina de la tabla la clave key (y su correspondiente valor asociado).
     * El método no hace nada si la clave no está en la tabla.
     * @param key la clave a eliminar.
     * @return El objeto al cual la clave estaba asociada, o null si la clave no
     *         estaba en la tabla.
     * @throws NullPointerException - if the key is null.
     */
    @Override
    public V remove(Object key) {
        if (key == null)
            throw new NullPointerException("remove(): parámetro null");

        int ih = this.h((K) key);
        int ic = ih;
        int j = 1;
        V old = null;


        while (this.states[ic] != 0) {


            if (this.states[ic] == 1) {
                Entry<K, V> entry = this.table[ic];

                if (key.equals(entry.getKey())) {
                    old = entry.getValue();
                    this.table[ic] = null;
                    this.states[ic] = 2;

                    this.count--;
                    this.modCount++;

                    return old;
                }
            }

            ic += j * j;
            j++;
            if (ic >= this.table.length) {
                ic %= this.table.length;
            }
        }

        return old;
    }

    /**
     * Copia en esta tabla, todos los objetos contenidos en el map especificado.
     * Los nuevos objetos reemplazarán a los que ya existan en la tabla
     * asociados a las mismas claves (si se repitiese alguna).
     * @param m el map cuyos objetos serán copiados en esta tabla.
     * @throws NullPointerException si m es null.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * Elimina todo el contenido de la tabla, de forma de dejarla vacía. En esta
     * implementación además, el arreglo de soporte vuelve a tener el tamaño que
     * inicialmente tuvo al ser creado el objeto.
     */
    @Override
    public void clear() {


        this.table = new Entry[this.initial_capacity];

        states = new int[this.initial_capacity];


        for (int i = 0; i < states.length; i++) {
            states[i] = 0;
        }

        this.count = 0;
        this.modCount++;
    }

    /**
     * Retorna un Set (conjunto) a modo de vista de todas las claves (key)
     * contenidas en la tabla. El conjunto está respaldado por la tabla, por lo
     * que los cambios realizados en la tabla serán reflejados en el conjunto, y
     * viceversa. Si la tabla es modificada mientras un iterador está actuando
     * sobre el conjunto vista, el resultado de la iteración será indefinido
     * (salvo que la modificación sea realizada por la operación remove() propia
     * del iterador, o por la operación setValue() realizada sobre una entrada
     * de la tabla que haya sido retornada por el iterador). El conjunto vista
     * provee métodos para eliminar elementos, y esos métodos a su vez
     * eliminan el correspondiente par (key, value) de la tabla (a través de las
     * operaciones Iterator.remove(), Set.remove(), removeAll(), retainAll()
     * y clear()). El conjunto vista no soporta las operaciones add() y
     * addAll() (si se las invoca, se lanzará una UnsuportedOperationException).
     * @return un conjunto (un Set) a modo de vista de todas las claves
     *         mapeadas en la tabla.
     */
    @Override
    public Set<K> keySet() {
        if (keySet == null) {
            // keySet = Collections.synchronizedSet(new KeySet());
            keySet = new KeySet();
        }
        return keySet;
    }

    /**
     * Retorna una Collection (colección) a modo de vista de todos los valores
     * (values) contenidos en la tabla. La colección está respaldada por la
     * tabla, por lo que los cambios realizados en la tabla serán reflejados en
     * la colección, y viceversa. Si la tabla es modificada mientras un iterador
     * está actuando sobre la colección vista, el resultado de la iteración será
     * indefinido (salvo que la modificación sea realizada por la operación
     * remove() propia del iterador, o por la operación setValue() realizada
     * sobre una entrada de la tabla que haya sido retornada por el iterador).
     * La colección vista provee métodos para eliminar elementos, y esos métodos
     * a su vez eliminan el correspondiente par (key, value) de la tabla (a
     * través de las operaciones Iterator.remove(), Collection.remove(),
     * removeAll(), removeAll(), retainAll() y clear()). La colección vista no
     * soporta las operaciones add() y addAll() (si se las invoca, se lanzará
     * una UnsuportedOperationException).
     * @return una colección (un Collection) a modo de vista de todas los
     *         valores mapeados en la tabla.
     */
    @Override
    public Collection<V> values() {
        if (values == null) {
            // values = Collections.synchronizedCollection(new ValueCollection());
            values = new ValueCollection();
        }
        return values;
    }

    /**
     * Retorna un Set (conjunto) a modo de vista de todos los pares (key, value)
     * contenidos en la tabla. El conjunto está respaldado por la tabla, por lo
     * que los cambios realizados en la tabla serán reflejados en el conjunto, y
     * viceversa. Si la tabla es modificada mientras un iterador está actuando
     * sobre el conjunto vista, el resultado de la iteración será indefinido
     * (salvo que la modificación sea realizada por la operación remove() propia
     * del iterador, o por la operación setValue() realizada sobre una entrada
     * de la tabla que haya sido retornada por el iterador). El conjunto vista
     * provee métodos para eliminar elementos, y esos métodos a su vez
     * eliminan el correspondiente par (key, value) de la tabla (a través de las
     * operaciones Iterator.remove(), Set.remove(), removeAll(), retainAll()
     * and clear()). El conjunto vista no soporta las operaciones add() y
     * addAll() (si se las invoca, se lanzará una UnsuportedOperationException).
     * @return un conjunto (un Set) a modo de vista de todos los objetos
     *         mapeados en la tabla.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        if (entrySet == null) {
            entrySet = new EntrySet();
        }
        return entrySet;
    }

    /**
     * Retorna una copia superficial de la tabla. Las listas de desborde o
     * buckets que conforman la tabla se clonan ellas mismas, pero no se clonan
     * los objetos que esas listas contienen: en cada bucket de la tabla se
     * almacenan las direcciones de los mismos objetos que contiene la original.
     * @return una copia superficial de la tabla.
     * @throws java.lang.CloneNotSupportedException si la clase no implementa la
     *         interface Cloneable.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        TSBHashtableDA<K, V> t = new TSBHashtableDA<>(this.table.length, this.load_factor);
        for (Map.Entry<K, V> entry : this.entrySet()) {
            t.put(entry.getKey(), entry.getValue());
        }

        return t;
    }

    /**
     * Determina si esta tabla es igual al objeto espeficicado.
     * @param obj el objeto a comparar con esta tabla.
     * @return true si los objetos son iguales.
     */

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Map)) {
            return false;
        }

        Map<K, V> t = (Map<K, V>) obj;
        if (t.size() != this.size()) {
            return false;
        }

        try {
            Iterator<Map.Entry<K, V>> i = this.entrySet.iterator();
            while (i.hasNext()) {
                Map.Entry<K, V> e = i.next();
                K key = e.getKey();
                V value = e.getValue();
                if (t.get(key) == null) {
                    return false;
                } else {
                    if (!value.equals(t.get(key))) {
                        return false;
                    }
                }
            }
        } catch (ClassCastException e) {
            return false;
        }

        return true;
    }

    /**
     * Retorna un hash code para la tabla completa.
     * @return un hash code para la tabla.
     */
    @Override
    public int hashCode() {
        if (this.isEmpty()) return 0;
        return Arrays.hashCode(this.table);
    }

    /**
     * Devuelve el contenido de la tabla en forma de String. Sólo por razones de
     * didáctica, se hace referencia explícita en esa cadena al contenido de
     * cada una de las listas de desborde o buckets de la tabla.
     * @return una cadena con el contenido completo de la tabla.
     */
    @Override
    public String toString() {
        StringBuilder cad = new StringBuilder("");
        cad.append("\nTabla: {\n");
        for (int i = 0; i < this.table.length; i++) {
            if (this.table[i] == null) {
                cad.append("\t()\n");
            } else {
                cad.append("\t").append(this.table[i].toString()).append("\n");
            }
        }
        cad.append("}");
        return cad.toString();
    }

    /**
     * Determina si alguna clave de la tabla está asociada al objeto value que
     * entra como parámetro. Equivale a containsValue().
     * @param value el objeto a buscar en la tabla.
     * @return true si alguna clave está asociada efectivamente a ese value.
     */
    public boolean contains(Object value) {
        if (value == null)
            return false;

        Iterator<Map.Entry<K, V>> it = this.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, V> entry = it.next();
            if (value.equals(entry.getValue()))
                return true;
        }

        return false;
    }

    /**
     * Incrementa el tamaño de la tabla y reorganiza su contenido. Se invoca
     * automaticamente cuando se detecta que la cantidad promedio de nodos por
     * lista supera a cierto el valor critico dado por (10 * load_factor). Si el
     * valor de load_factor es 0.8, esto implica que el límite antes de invocar
     * rehash es de 8 nodos por lista en promedio, aunque seria aceptable hasta
     * unos 10 nodos por lista.
     */
    protected void rehash() {
        int old_length = this.table.length;
        int new_length = siguientePrimo(old_length * 2 + 1);
        if (new_length > TSBHashtableDA.MAX_SIZE)
            new_length = TSBHashtableDA.MAX_SIZE;
        Entry<K, V> tempTable[] = new Entry[new_length];
        int tempStates[] = new int[new_length];
        for (int i = 0; i < tempStates.length; i++) tempStates[i] = 0;
        this.modCount++;
        for (int i = 0; i < this.table.length; i++) {
            if (this.states[i] == 1) {

                Entry<K, V> x = this.table[i];

                K key = x.getKey();
                int y = this.h(key, tempTable.length);
                int ic = y, j = 1;
                while (tempStates[ic] != 0) {
                    ic += j * j;
                    j++;
                    if (ic >= tempTable.length) {
                        ic %= tempTable.length;
                    }
                }

                tempTable[ic] = x;
                tempStates[ic] = 1;
            }
        }

        this.table = tempTable;
        this.states = tempStates;
    }

    /*
     * Función hash. Toma una clave entera k y calcula y retorna un índice
     * válido para esa clave para entrar en la tabla.
     */
    private int h(int k) {
        return h(k, this.table.length);
    }

    /*
     * Función hash. Toma un objeto key que representa una clave y calcula y
     * retorna un índice válido para esa clave para entrar en la tabla.
     */
    private int h(K key) {
        return h(key.hashCode(), this.table.length);
    }

    /*
     * Función hash. Toma un objeto key que representa una clave y un tamaño de
     * tabla t, y calcula y retorna un índice válido para esa clave dedo ese
     * tamaño.
     */
    private int h(K key, int t) {
        return h(key.hashCode(), t);
    }

    /*
     * Función hash. Toma una clave entera k y un tamaño de tabla t, y calcula y
     * retorna un índice válido para esa clave dado ese tamaño.
     */
    private int h(int k, int t) {
        if (k < 0)
            k *= -1;
        return k % t;
    }



    /*
     * Clase interna que representa los pares de objetos que se almacenan en la
     * tabla hash: son instancias de esta clase las que realmente se guardan en
     * en cada una de las listas del arreglo table que se usa como soporte de
     * la tabla. Lanzará una IllegalArgumentException si alguno de los dos
     * parámetros es null.
     */

    private class Entry<K, V> implements Map.Entry<K, V> {
        private K key;
        private V value;

        public Entry(K key, V value) {
            if (key == null || value == null) {
                throw new IllegalArgumentException("Entry(): parámetro null...");
            }
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            if (value == null) {
                throw new IllegalArgumentException("setValue(): parámetro null...");
            }

            V old = this.value;
            this.value = value;
            return old;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 61 * hash + Objects.hashCode(this.key);
            hash = 61 * hash + Objects.hashCode(this.value);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (this.getClass() != obj.getClass()) {
                return false;
            }

            final Entry other = (Entry) obj;
            if (!Objects.equals(this.key, other.key)) {
                return false;
            }
            if (!Objects.equals(this.value, other.value)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "(" + key.toString() + ", " + value.toString() + ")";
        }
    }

    private transient Set<K> keySet = null;
    private transient Set<Map.Entry<K, V>> entrySet = null;
    private transient Collection<V> values = null;

    /*
     * Clase interna que representa una vista de todas los Claves mapeadas en la
     * tabla: si la vista cambia, cambia también la tabla que le da respaldo, y
     * viceversa. La vista es stateless: no mantiene estado alguno (es decir, no
     * contiene datos ella misma, sino que accede y gestiona directamente datos
     * de otra fuente), por lo que no tiene atributos y sus métodos gestionan en
     * forma directa el contenido de la tabla. Están soportados los metodos para
     * eliminar un objeto (remove()), eliminar todo el contenido (clear) y la
     * creación de un Iterator (que incluye el método Iterator.remove()).
     */

    private class KeySet extends AbstractSet<K> {
        @Override
        public Iterator<K> iterator() {
            return new KeySetIterator();
        }

        @Override
        public int size() {
            return TSBHashtableDA.this.count;
        }

        @Override
        public boolean contains(Object o) {
            return TSBHashtableDA.this.containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            return (TSBHashtableDA.this.remove(o) != null);
        }

        @Override
        public void clear() {
            TSBHashtableDA.this.clear();
        }

        private class KeySetIterator implements Iterator<K> {


            private int last_entry;


            private int current_entry;


            private boolean next_ok;


            private int expected_modCount;


            public KeySetIterator() {
                last_entry = 0;
                current_entry = -1;
                next_ok = false;
                expected_modCount = TSBHashtableDA.this.modCount;
            }


            @Override
            public boolean hasNext() {
                Entry<K, V> t[] = TSBHashtableDA.this.table;
                int s[] = TSBHashtableDA.this.states;

                if (current_entry >= t.length) {
                    return false;
                }

                int next_entry = current_entry + 1;
                for (int i = next_entry; i < t.length; i++) {
                    if (s[i] == 1) return true;
                }

                return false;
            }

            @Override
            public K next() {

                if (TSBHashtableDA.this.modCount != expected_modCount) {
                    throw new ConcurrentModificationException("next(): modificación inesperada de tabla...");
                }
                if (!hasNext()) {
                    throw new NoSuchElementException("next(): no existe el elemento pedido...");
                }
                Entry<K, V> t[] = TSBHashtableDA.this.table;
                int s[] = TSBHashtableDA.this.states;
                int next_entry = current_entry;
                for (next_entry++; s[next_entry] != 1; next_entry++) ;
                last_entry = current_entry;
                current_entry = next_entry;
                next_ok = true;
                K key = t[current_entry].getKey();
                return key;
            }

            @Override
            public void remove() {
                // control: fail-fast iterator...
                if (TSBHashtableDA.this.modCount != expected_modCount) {
                    throw new ConcurrentModificationException("remove(): modificación inesperada de tabla...");
                }

                if (!next_ok) {
                    throw new IllegalStateException("remove(): debe invocar a next() antes de remove()...");
                }

                TSBHashtableDA.this.table[current_entry] = null;
                TSBHashtableDA.this.states[current_entry] = 2;
                current_entry = last_entry;
                next_ok = false;
                TSBHashtableDA.this.count--;
                TSBHashtableDA.this.modCount++;
                expected_modCount++;
            }
        }
    }

    /*
     * Clase interna que representa una vista de todos los PARES mapeados en la
     * tabla: si la vista cambia, cambia también la tabla que le da respaldo, y
     * viceversa. La vista es stateless: no mantiene estado alguno (es decir, no
     * contiene datos ella misma, sino que accede y gestiona directamente datos
     * de otra fuente), por lo que no tiene atributos y sus métodos gestionan en
     * forma directa el contenido de la tabla. Están soportados los metodos para
     * eliminar un objeto (remove()), eliminar todo el contenido (clear) y la
     * creación de un Iterator (que incluye el método Iterator.remove()).
     */
    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntrySetIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (o == null) {
                return false;
            }
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry<K, V> t[] = TSBHashtableDA.this.table;
            int s[] = TSBHashtableDA.this.states;

            Entry<K, V> entry = (Entry<K, V>) o;

            int ih = TSBHashtableDA.this.h(entry.getKey());
            int ic = ih;
            int j = 1;

            while (s[ic] != 0) {
                if (s[ic] == 1) {
                    Entry<K, V> entryTable = t[ic];
                    if (entryTable.equals(entry)) return true;
                }
                ic += j * j;
                j++;
                if (ic >= t.length) {
                    ic %= t.length;
                }
            }
            return false;
        }

        @Override
        public boolean remove(Object o) {
            if (o == null) {
                throw new NullPointerException("remove(): parámetro null");
            }
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry<K, V> t[] = TSBHashtableDA.this.table;
            int s[] = TSBHashtableDA.this.states;
            Entry<K, V> entry = (Entry<K, V>) o;
            int ih = TSBHashtableDA.this.h(entry.getKey());
            int ic = ih;
            int j = 1;

            while (s[ic] != 0) {
                if (s[ic] == 1) {
                    Entry<K, V> entryTable = t[ic];
                    if (entryTable.equals(entry)) {
                        t[ic] = null;
                        s[ic] = 2;

                        TSBHashtableDA.this.count--;
                        TSBHashtableDA.this.modCount++;

                        return true;
                    }
                }
                ic += j * j;
                j++;
                if (ic >= t.length) {
                    ic %= t.length;
                }
            }
            return false;
        }

        @Override
        public int size() {
            return TSBHashtableDA.this.count;
        }

        @Override
        public void clear() {
            TSBHashtableDA.this.clear();
        }

        private class EntrySetIterator implements Iterator<Map.Entry<K, V>> {
            private int last_entry;
            private int current_entry;
            private boolean next_ok;
            private int expected_modCount;

            public EntrySetIterator() {
                last_entry = 0;
                current_entry = -1;
                next_ok = false;
                expected_modCount = TSBHashtableDA.this.modCount;
            }

            @Override
            public boolean hasNext() {
                Entry<K, V> t[] = TSBHashtableDA.this.table;
                int s[] = TSBHashtableDA.this.states;

                if (current_entry >= t.length) {
                    return false;
                }
                int next_entry = current_entry + 1;
                for (int i = next_entry; i < t.length; i++) {
                    if (s[i] == 1) return true;
                }
                return false;
            }

            @Override
            public Entry<K, V> next() {
                if (TSBHashtableDA.this.modCount != expected_modCount) {
                    throw new ConcurrentModificationException("next(): modificación inesperada de tabla...");
                }

                if (!hasNext()) {
                    throw new NoSuchElementException("next(): no existe el elemento pedido...");
                }
                Entry<K, V> t[] = TSBHashtableDA.this.table;
                int s[] = TSBHashtableDA.this.states;
                int next_entry = current_entry;
                for (next_entry++; s[next_entry] != 1; next_entry++) ;
                last_entry = current_entry;
                current_entry = next_entry;
                next_ok = true;

                // y retornar el entry alcanzado...
                return t[current_entry];
            }

            @Override
            public void remove() {
                if (!next_ok) {
                    throw new IllegalStateException("remove(): debe invocar a next() antes de remove()...");
                }
                TSBHashtableDA.this.table[current_entry] = null;
                TSBHashtableDA.this.states[current_entry] = 2;
                current_entry = last_entry;
                next_ok = false;
                TSBHashtableDA.this.count--;
                TSBHashtableDA.this.modCount++;
                expected_modCount++;
            }
        }
    }

    private class ValueCollection extends AbstractCollection<V> {
        @Override
        public Iterator<V> iterator() {
            return new ValueCollectionIterator();
        }

        @Override
        public int size() {
            return TSBHashtableDA.this.count;
        }

        @Override
        public boolean contains(Object o) {
            return TSBHashtableDA.this.containsValue(o);
        }

        @Override
        public void clear() {
            TSBHashtableDA.this.clear();
        }

        /*
         * Clase interna que representa una vista de todos los VALORES mapeados en
         * la tabla: si la vista cambia, cambia también la tabla que le da respaldo,
         * y viceversa. La vista es stateless: no mantiene estado alguno (es decir,
         * no contiene datos ella misma, sino que accede y gestiona directamente los
         * de otra fuente), por lo que no tiene atributos y sus métodos gestionan en
         * forma directa el contenido de la tabla. Están soportados los metodos para
         * eliminar un objeto (remove()), eliminar todo el contenido (clear) y la
         * creación de un Iterator (que incluye el método Iterator.remove()).
         */
        private class ValueCollectionIterator implements Iterator<V> {
            private int last_entry;
            private int current_entry;
            private boolean next_ok;
            private int expected_modCount;

            public ValueCollectionIterator() {
                last_entry = 0;
                current_entry = -1;
                next_ok = false;
                expected_modCount = TSBHashtableDA.this.modCount;
            }

            @Override
            public boolean hasNext() {
                Entry<K, V> t[] = TSBHashtableDA.this.table;
                int s[] = TSBHashtableDA.this.states;

                if (current_entry >= t.length) {
                    return false;
                }

                int next_entry = current_entry + 1;
                for (int i = next_entry; i < t.length; i++) {
                    if (s[i] == 1) return true;
                }
                return false;
            }

            @Override
            public V next() {
                if (TSBHashtableDA.this.modCount != expected_modCount) {
                    throw new ConcurrentModificationException("next(): modificación inesperada de tabla...");
                }

                if (!hasNext()) {
                    throw new NoSuchElementException("next(): no existe el elemento pedido...");
                }
                Entry<K, V> t[] = TSBHashtableDA.this.table;
                int s[] = TSBHashtableDA.this.states;
                int next_entry = current_entry;
                for (next_entry++; s[next_entry] != 1; next_entry++) ;
                last_entry = current_entry;
                current_entry = next_entry;
                next_ok = true;
                V value = t[current_entry].getValue();

                return value;
            }

            @Override
            public void remove() {
                // control: fail-fast iterator...
                if (TSBHashtableDA.this.modCount != expected_modCount) {
                    throw new ConcurrentModificationException("remove(): modificación inesperada de tabla...");
                }

                if (!next_ok) {
                    throw new IllegalStateException("remove(): debe invocar a next() antes de remove()...");
                }
                TSBHashtableDA.this.table[current_entry] = null;
                TSBHashtableDA.this.states[current_entry] = 2;
                current_entry = last_entry;
                next_ok = false;
                TSBHashtableDA.this.count--;
                TSBHashtableDA.this.modCount++;
                expected_modCount++;
            }
        }
    }

    private int siguientePrimo(int n) {
        if (n % 2 == 0) n++;
        for (; !esPrimo(n); n += 2) ;
        return n;
    }

    private boolean esPrimo(int n) {
        for (int i = 3; i < (int) Math.sqrt(n); i += 2) {
            if (n % i == 0) return false;
        }
        return true;
    }
}
