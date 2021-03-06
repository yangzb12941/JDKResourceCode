package java.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import sun.misc.SharedSecrets;
/**
 * 基于哈希表的Map接口的实现。此实现提供所有可选的映射操作，并允许空值和空键。
 * （HashMap类与Hashtable大致等效，不同之处在于它是不x线程安全的，并且允许为null。）该类不保证映射的顺序。
 * 特别是，它不能保证顺序会随着时间的推移保持恒定。
 * 假设哈希函数将元素正确分散在存储桶中，则此实现为基本操作（获取和放置）提供恒定时间的性能。
 * 集合视图上的迭代所需的时间与HashMap实例的“容量”（存储桶数）及其大小（键-值映射数）成正比。
 * 因此，如果迭代性能很重要，则不要将初始容量设置得过高（或负载因子过低），这一点非常重要。
 * HashMap的实例具有两个影响其性能的参数：初始容量和负载因子。容量是哈希表中存储桶的数量，
 * 初始容量只是创建哈希表时的容量。负载因子是在自动增加其哈希表容量之前允许哈希表获得的满度的度量。
 * 当哈希表中的条目数超过负载因子和当前容量的乘积时，哈希表将被重新哈希（即，内部数据结构将被重建），
 * 此时哈希表的存储容量扩大为之前的两倍。
 * 通常，默认负载因子（0.75）在时间和空间成本之间提供了一个很好的折衷方案。较高的值会减少空间开销，
 * 但会增加查找成本（在HashMap类的大多数操作中都得到体现，包括get和put）。设置映射表的初始容量时，
 * 应考虑映射中的预期条目数及其负载因子，以最大程度地减少重新哈希操作的次数。如果初始容量大于最大条目数除以负载因子，
 * 则将不会进行任何rehash操作。
 * 如果要将许多映射存储在HashMap实例中，则创建具有足够大容量的映射将比使它根据需要增长表的自动重新哈希处理更有效地存储映射。
 * 请注意，使用许多具有相同hashCode()的键会降低任何哈希表性能的。为了改善影响，当键是Comparable时，
 * 此类可以使用键之间的比较顺序来帮助打破关系。
 * 请注意，此HashMap未实现多线程安全。如果多个线程同时访问哈希映射，并且至少有一个线程在结构上修改该映射，则必须在外部进行同步。
 * （结构修改是添加或删除一个或多个映射的任何操作；仅更改与实例已经包含的键相关联的值不是结构修改。）
 * 通常通过在自然封装了Map的某个对象上进行同步来实现。
 * 如果不存在这样的对象，则应使用Collections.synchronizedMap方法“包装”Map。
 * 最好在创建时完成此操作，以防止意外不同步地访问Map：
 *      map = Collections.synchronizedMap（new HashMap（...））;
 * 由此类的所有“集合视图方法”返回的迭代器都是快速失败的：如果在创建迭代器后的任何时候以任何方式对映射进行结构修改，
 * 则除了通过迭代器自己的remove方法之外，迭代器都会抛出ConcurrentModificationException 。
 * 因此，面对并发修改，迭代器会快速地失败(没有实现线程安全的集合类，都会有快速失败机制)，而不会在未来的不确定时间内冒任意，
 * 不确定的行为的风险。
 * 请注意，迭代器的快速失败行为无法得到保证，因为通常来说，在存在不同步的并发修改的情况下，不可能做出任何严格的保证。
 * 快速失败的迭代器会尽最大努力抛出ConcurrentModificationException。
 * 因此，编写依赖于此异常的程序以确保其正确性是错误的：迭代器的快速失败行为应仅用于检测错误。
 * 此类是Java Collections Framework的成员。
 * @author  Doug Lea
 * @author  Josh Bloch
 * @author  Arthur van Hoff
 * @author  Neal Gafter
 * @see     Object#hashCode()
 * @see     Collection
 * @see     Map
 * @see     TreeMap
 * @see     Hashtable
 * @since   1.2
 */
public class HashMap<K,V> extends AbstractMap<K,V>
    implements Map<K,V>, Cloneable, Serializable {
    private static final long serialVersionUID = 362498820763181265L;


    /**
     * 默认初始容量，即桶的初始容量 16(必须是2的幂次方)
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

    /**
     * 桶的最大容量2的30次方 1,073,741,824‬
     * 二进制：‭01000000000000000000000000000000‬
     * 为什么不是 1 << 31 结果：-2147483648
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 负载因子，用来计算resize 阈值
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * 链表转化成树的阈值，当桶中的链表长度大于8时转化成树
     * threshold = capacity * loadFactor
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * 进行resize操作时，若桶中数量少于6则从树转成链表
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * 桶中结构转化为红黑树对应的table的最小大小
     *
     *  当需要将解决 hash 冲突的链表转变为红黑树时，
     *  需要判断下此时数组容量，
     *  若是由于数组容量太小（小于　MIN_TREEIFY_CAPACITY 64　）
     *  导致的 hash 冲突太多，则不进行链表转变为红黑树操作，
     *  转为利用　resize() 函数对　hashMap 扩容。
     *  为什么在一个链上出现8个节点，需要转化为红黑树的时候，需要判断桶的大小
     *  是否超过64？能不使用红黑树，就尽量不使用红黑树
     *  1、因为红黑树的每个节点消耗内存是普通节点的两倍；
     *  2、红黑树需要通过左右旋转来达到平衡；增加操作的复杂性
     */
    static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     * 保存Node<K,V>节点的数组，即"桶"
     *  该数组在首次使用时初始化，并根据需要调整大小。 分配时，
     *  长度始终是2的幂。
     */
    transient java.util.HashMap.Node<K,V>[] table;

    /**
     * 存放具体元素的集
     */
    transient Set<Entry<K,V>> entrySet;

    /**
     * 记录 hashMap 当前存储的元素的数量
     */
    transient int size;

    /**
     * 每次更改map结构的计数器
     */
    transient int modCount;

    /**
     * 临界值 当实际大小(容量*填充因子)超过临界值时，会进行扩容
     */
    int threshold;

    /**
     * 负载因子：要调整大小的下一个大小值（容量*加载因子）。
     * 对于 HashMap 来说，负载因子是一个很重要的参数，该参数反应了 HashMap 桶数组的使用情况。
     * 通过调节负载因子，可使 HashMap 时间和空间复杂度上有不同的表现。
     */
    final float loadFactor;

    /**
     * 节点数据结构
     */
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;
        final K key;
        V value;
        java.util.HashMap.Node<K, V> next;//用单向链表解决hash碰撞问题

        Node(int hash, K key, V value, java.util.HashMap.Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey() {
            return key;
        }

        public final V getValue() {
            return value;
        }

        public final String toString() {
            return key + "=" + value;
        }

        //节点对象的hashCode,通过key对象的hashCode和value对象的hashCode 异或操作得到。
        //重写hashCode：Object的hashCode直接比较的是对象头中25位hash值。
        //而Map<key,value>结构是根据Key\value的值比较是否相等。所以不能直接用
        //Object的hashCode方法。
        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        //给节点设置新值时，会返回旧值
        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        //重写equals，Object的equals直接比较的是对象在内存中的地址。
        //而Map<key,value>结构是根据Key\value的值比较是否相等。所以不能直接用
        //Object的equals方法。原因与hashCode方法重写是一致的。
        public final boolean equals(Object o) {
            if (o == this)
                return true;
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                if (Objects.equals(key, e.getKey()) &&
                        Objects.equals(value, e.getValue()))
                    return true;
            }
            return false;
        }
    }

    /**
     * 1、key 为null 时定位在桶中位置的hash值为0；
     * 2、不为0时，key对象的hashCode高16位异或低16位作为映射到
     * 桶中的hash值。
     * 3、为什么使用高16位异或低16位呢？
     * 每一位出现0\1d都是1/2 得概率，而且
     * 1^1 = 0
     * 1^0 = 1
     * 0^0 = 0
     * 0^1 = 1
     * 这四种情况异或出来的0,1是很均匀的。
     *
     * 这里有个疑问？对象的hashCode不是对象头中25位吗，按理来说高位的前7位都是
     * 0000000才对，而Integer.toBinaryString(t.hashCode())却显示高位不为0
     * 1101000111101111010101011100010
     */
    static final int hash(Object key) {
        int h;
        //拿到key的hash值后无符号右移16位取与操作
        // 通过这种方式，让高位数据与低位数据进行异或，以此加大低位信息的随机性，变相的让高位数据参与到计算中。
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    /**
     * 返回x的实现Comparable<C>接口的Class类，否则返回空.
     * 用于插入TreeNode时进行比较大小。因为红黑树是自平衡的
     * 顺序树，插入红黑树的节点之间需要能比较大小。
     */
    static Class<?> comparableClassFor(Object x) {
        if (x instanceof Comparable) {
            Class<?> c; Type[] ts, as; Type t; ParameterizedType p;
            //x是string 类，直接返回String类。
            if ((c = x.getClass()) == String.class) // bypass checks
                return c;
            //通过反射获取c类实现的接口
            if ((ts = c.getGenericInterfaces()) != null) {
                for (int i = 0; i < ts.length; ++i) {
                    if (((t = ts[i]) instanceof ParameterizedType) &&
                            ((p = (ParameterizedType)t).getRawType() ==
                                    Comparable.class) &&
                            (as = p.getActualTypeArguments()) != null &&
                            as.length == 1 && as[0] == c) // type arg is c
                        return c;
                }
            }
        }
        return null;
    }

    /**
     * 如果x匹配kc（k筛选的可比对象，则返回k.compareTo（x类），否则为0。
     */
    @SuppressWarnings({"rawtypes","unchecked"}) // for cast to Comparable
    static int compareComparables(Class<?> kc, Object k, Object x) {
        return (x == null || x.getClass() != kc ? 0 :
                ((Comparable)k).compareTo(x));
    }

    /**
     * 用位运算找到大于或等于 cap 的最小2的整数次幂的数。比如：13，则获取16.
     * 为什么cap需要-1呢？
     * 1、让cap-1再赋值给n的目的是使得找到的目标值大于或等于原值。例如二进制0100,十进制是4,
     * 若不减1而直接操作，答案是0001 0000十进制是16，明显不符合预期。
     * 2、对n右移1位：001xx...xxx，再位或：011xx...xxx
     * 3、对n右移2位：00011...xxx，再位或：01111...xxx
     * 4、对n右移4位...
     * 5、对n右移8位...
     * 6、对n右移16位,因为int最大就2^32所以移动1、2、4、8、16位并取位或,会将最高位的1后面的位全变为1。
     * 7、再让结果n+1，即得到了2的整数次幂的值了。
     */
    static final int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        //超过最大，取最大；
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }
    /**
     * 传入初始容量大小和负载因子 来初始化HashMap对象
     */
    public HashMap(int initialCapacity, float loadFactor) {
        // 初始容量不能小于0，否则报错
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                    initialCapacity);
        // 初始容量不能大于最大值，否则为最大值
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        //负载因子不能小于或等于0，不能为非数字
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                    loadFactor);
        // 初始化负载因子，负载因子可以大于1(默认的负载因子是0.75)
        // 当负载因子大于1时，hashMap存储的数据就多，空间利用率也更高；
        // 但发生hash碰撞以及hash链也长，查找效率变低，典型的用时间换空间。

        // 当负载因子表较小的时候，hashMap存储的数据也变少，空间利用率下降。但发生hash
        // 碰撞的几率变小，碰撞链也变短，查找效率变高。典型的用空间换时间。
        this.loadFactor = loadFactor;
        // 初始化threshold大小
        this.threshold = tableSizeFor(initialCapacity);
    }

    /**
     * 待初始容量的构造方法，负载因子默认为0.75
     */
    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * 不参数的构造方法，默认负载因子为0.75；初始容量为 16.
     * 注意：实例化hashMap的时候，并没有初始化桶！！！
     */
    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
    }

    /**
     * 用指定的Map构造hashMap,负载因子只要没有人为更改，都默认为
     * 0.75
     */
    public HashMap(Map<? extends K, ? extends V> m) {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        putMapEntries(m, false);
    }

    /**
     * 把Map元素填装进hashMap中。
     */
    final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
        int s = m.size();
        if (s > 0) {
            //table为空，表明hashMap还没被初始化
            if (table == null) { // pre-size
                //使用Map的size 计算出HashMap的容量。
                float ft = ((float)s / loadFactor) + 1.0F;
                int t = ((ft < (float)MAXIMUM_CAPACITY) ?
                        (int)ft : MAXIMUM_CAPACITY);
                if (t > threshold)
                    threshold = tableSizeFor(t);
            }
            else if (s > threshold)
                //大于临界容量，则直接resize操作
                resize();
            //取出Map中的数据，一个一个Put到hashMap中。
            for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
                K key = e.getKey();
                V value = e.getValue();
                //为key计算hash值。
                putVal(hash(key), key, value, false, evict);
            }
        }
    }

    /**
     * 获取当前Key-Value 键值对的数量
     */
    public int size() {
        return size;
    }

    /**
     * hashMap是否为空，键值对数量 == 0 则表明没有数据，为空。
     * table 也为null
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 根据key获取值；
     */
    public V get(Object key) {
        java.util.HashMap.Node<K,V> e;
        //1、先获取key映射到桶位置的hash值；
        //2、通过hash值和key的值找到对应的Node节点，取出对应的value；
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }

    /**
     * 获取key对应value 的核心方法
     */
    final java.util.HashMap.Node<K,V> getNode(int hash, Object key) {
        java.util.HashMap.Node<K,V>[] tab; java.util.HashMap.Node<K,V> first, e; int n; K k;
        //tab  = table 这句话使对象保留在当前线程执行栈的局部变量表中。
        //若是别线程操作table = null,而当前线程中的tab还是有效的。

        //tab.length 就是数组长度，tab[(n-1)&hash]等效于 hash % tab.length操作
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (first = tab[(n - 1) & hash]) != null) {
            //查看桶中第一个节点是不是需要查找的节点
            //1、比较节点hash是否相等；
            //2、比较key值是否相等；这里隐藏了一个细节点：
            // Java 中equals()方法，对于基本数据类型比较的是值；对于引用对象类型，比较的是
            // 对象内存地址。若key是基本数据类型的包装类型，由于重写equals()的缘故，比较的也是值
            // 是否相等。

            // 若是k是普通的对象，且内部有字段。而改变对象内部字段值，不会使对象hash值、对象地址变更。
            // 所以，一般key采用string 或基本数据类型的包装类型。或是重写equals()方法。
            if (first.hash == hash && // always check first node
                    ((k = first.key) == key || (key != null && key.equals(k))))
                return first;
            //第一个节点不是，且后面还链了其他节点。
            if ((e = first.next) != null) {
                //第一个链是TreeNode类型，说明是红黑树
                if (first instanceof java.util.HashMap.TreeNode)
                    //在红黑树中查找
                    return ((java.util.HashMap.TreeNode<K,V>)first).getTreeNode(hash, key);
                do {
                    //在链中查找，链是一个单向链表，需要从头节点开始找。
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }

    /**
     * 查找是否包含某个key值
     */
    public boolean containsKey(Object key) {
        return getNode(hash(key), key) != null;
    }

    /**
     *往hashMap中放key,value
     */
    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }

    /**
     * hashMap 核心put操作
     * onlyIfAbsent 参数，默认为false, 表明若是存在相同的节点，则更新值；
     * 若是true 则表明相同节点不更新值
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        java.util.HashMap.Node<K,V>[] tab; java.util.HashMap.Node<K,V> p; int n, i;
        //当前桶数组为null或同数组长度为0，那么进入resize方法。
        //tab  = table 这句话使对象保留在当前线程执行栈的局部变量表中。
        //若是别线程操作table = null,而当前线程中的tab还是有效的。
        if ((tab = table) == null || (n = tab.length) == 0)
            //进入resize方法
            n = (tab = resize()).length;
        //tab[i = (n - 1) & hash],其中的(n - 1) & hash 就是取余hash%n
        //当前桶位置是空
        if ((p = tab[i = (n - 1) & hash]) == null)
            //新建一个node放在对应的i位置上。
            tab[i] = newNode(hash, key, value, null);
        else {
            //对应位置上有值，则直接判断是否为相同的node
            java.util.HashMap.Node<K,V> e; K k;
            if (p.hash == hash &&
                    ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;
            //如果是红黑树节点，则放入树中。
            else if (p instanceof java.util.HashMap.TreeNode)
                e = ((java.util.HashMap.TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            else {
                for (int binCount = 0; ; ++binCount) {
                    if ((e = p.next) == null) {
                        //hash碰撞之后，1.8 采用尾插法在链中加入新数据；而1.7是采用头插法；
                        //为什么呢？
                        //因为头插法在多线程扩容的情况下，有可能会导致形成环，这样就造成线程死循环。
                        //而尾插法能避免这种问题。
                        p.next = newNode(hash, key, value, null);

                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);
                        break;
                    }
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    p = e;
                }
            }
            if (e != null) { // existing mapping for key
                V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                //空方法，节点访问之后的处理，这个在LinkHashMap中有实现。
                afterNodeAccess(e);
                return oldValue;
            }
        }
        //hashMap结构被改变需要加1，用在容器的快速失败
        ++modCount;
        //当前的
        if (++size > threshold)
            resize();
        afterNodeInsertion(evict);
        return null;
    }

    /**
     * 1、put数据的时候，桶的初始化尽然是在resize方法中被初始化
     * 2、也即，调用resize方法并不都是需要扩容才调用，第一次put元素时也会被调用
     */
    final java.util.HashMap.Node<K,V>[] resize() {
        //tab  = table 这句话使对象保留在当前线程执行栈的局部变量表中。
        //若是别线程操作table = null,而当前线程中的tab还是有效的。
        java.util.HashMap.Node<K,V>[] oldTab = table;
        //当前桶没有被初始化，则旧初始容量为0
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        //resize操作的阈值(容量*负载因子)
        int oldThr = threshold;
        int newCap, newThr = 0;
        if (oldCap > 0) {
            //是否超过做大容量
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            //新容量 为 旧容量的 2倍
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                    oldCap >= DEFAULT_INITIAL_CAPACITY)
                //resize的阈值也是旧阈值的两倍
                newThr = oldThr << 1; // double threshold
        }
        else if (oldThr > 0) // initial capacity was placed in threshold
            //oldCap == 0 表明通过HashMap(int initialCapacity)构造函数生成的hashMap对象
            // 只有resize阈值被初始化为16，且当前是put第一个值则
            //初始化容量为16
            newCap = oldThr;
        else {               // zero initial threshold signifies using defaults
            //oldCap == 0 表明通过HashMap()构造函数生成的hashMap对象，且当前是put第一个值
            //初始化容量为16
            newCap = DEFAULT_INITIAL_CAPACITY;
            //新的resize 阈值，有可能出现小数，强转为int丢掉小数部分
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                    (int)ft : Integer.MAX_VALUE);
        }
        //更新hashMap对象的resize 阈值
        threshold = newThr;
        @SuppressWarnings({"rawtypes","unchecked"})
        //真正初始化桶数组
                java.util.HashMap.Node<K,V>[] newTab = (java.util.HashMap.Node<K,V>[])new java.util.HashMap.Node[newCap];
        //把数组赋值给对象变量，table。因为newTab是在局部变量表中的对象。
        //随着方法执行完就被回收。需要通过table 维持同数组的对象引用。
        table = newTab;
        //oldTab != null 表明是扩容调用resize方法
        if (oldTab != null) {
            for (int j = 0; j < oldCap; ++j) {
                java.util.HashMap.Node<K,V> e;
                //1、遍历同数组的每一个位置
                if ((e = oldTab[j]) != null) {
                    //这句话是为了优化GC
                    oldTab[j] = null;
                    //表明当前节点没有后继，则没有hash碰撞链表
                    if (e.next == null)
                        //直接把元素rehash到新的同数组中
                        newTab[e.hash & (newCap - 1)] = e;
                    else if (e instanceof java.util.HashMap.TreeNode)
                        //在rehash过程中，若是某个桶的节点是树节点结构，则需要进行特殊处理：
                        //①、整颗红黑树被拆解为两个双向链表，高位和低位双向链表；
                        //②、判断两个双向链表的元素个数是否大于6，若是则再形成新的红黑树；
                        // 否则拆解为普通的链表结构
                        ((java.util.HashMap.TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    else { // preserve order
                        java.util.HashMap.Node<K,V> loHead = null, loTail = null;
                        java.util.HashMap.Node<K,V> hiHead = null, hiTail = null;
                        java.util.HashMap.Node<K,V> next;
                        do {
                            next = e.next;
                            //把旧hash链拆解为高位和低位链
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            }
                            else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        //低位链还是在原 桶位置
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        //高位链在原桶位置+旧hash容量(因为hash扩容是2幂次方增长)
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }

        return newTab;
    }

    /**
     * Replaces all linked nodes in bin at index for given hash unless
     * table is too small, in which case resizes instead.
     */
    final void treeifyBin(java.util.HashMap.Node<K,V>[] tab, int hash) {
        int n, index; java.util.HashMap.Node<K,V> e;
        //当一个链的长度为8时，且桶的个数大于64，那么就把链转换成红黑树
        //若桶的数量没有达到64个，那么只进行扩容处理
        if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
            resize();
        else if ((e = tab[index = (n - 1) & hash]) != null) {
            java.util.HashMap.TreeNode<K,V> hd = null, tl = null;
            do {
                java.util.HashMap.TreeNode<K,V> p = replacementTreeNode(e, null);
                if (tl == null)
                    hd = p;
                else {
                    p.prev = tl;
                    tl.next = p;
                }
                tl = p;
            } while ((e = e.next) != null);
            //上面的逻辑是把普通节点转换成红黑树节点，并且形成双向链表，
            //这个双向链表的指向关系在转化成树时还是保留的。
            //这样有利于在rehash的过程中遍历整颗树。
            if ((tab[index] = hd) != null)
                //把上面的双向链表转化成红黑树
                hd.treeify(tab);
        }
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for
     * any of the keys currently in the specified map.
     *
     * @param m mappings to be stored in this map
     * @throws NullPointerException if the specified map is null
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        putMapEntries(m, true);
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param  key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    public V remove(Object key) {
        java.util.HashMap.Node<K,V> e;
        return (e = removeNode(hash(key), key, null, false, true)) == null ?
                null : e.value;
    }

    /**
     * Implements Map.remove and related methods
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to match if matchValue, else ignored
     * @param matchValue if true only remove if value is equal
     * @param movable if false do not move other nodes while removing
     * @return the node, or null if none
     */
    final java.util.HashMap.Node<K,V> removeNode(int hash, Object key, Object value,
                                                 boolean matchValue, boolean movable) {
        java.util.HashMap.Node<K,V>[] tab; java.util.HashMap.Node<K,V> p; int n, index;
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (p = tab[index = (n - 1) & hash]) != null) {
            java.util.HashMap.Node<K,V> node = null, e; K k; V v;
            if (p.hash == hash &&
                    ((k = p.key) == key || (key != null && key.equals(k))))
                node = p;
            else if ((e = p.next) != null) {
                if (p instanceof java.util.HashMap.TreeNode)
                    node = ((java.util.HashMap.TreeNode<K,V>)p).getTreeNode(hash, key);
                else {
                    do {
                        if (e.hash == hash &&
                                ((k = e.key) == key ||
                                        (key != null && key.equals(k)))) {
                            node = e;
                            break;
                        }
                        p = e;
                    } while ((e = e.next) != null);
                }
            }
            if (node != null && (!matchValue || (v = node.value) == value ||
                    (value != null && value.equals(v)))) {
                if (node instanceof java.util.HashMap.TreeNode)
                    //自己把自己从树节点移除
                    ((java.util.HashMap.TreeNode<K,V>)node).removeTreeNode(this, tab, movable);
                else if (node == p)
                    //桶的第一个元素，next可以是null
                    tab[index] = node.next;
                else
                    //链中元素
                    p.next = node.next;
                ++modCount;
                --size;
                //移除元素的后续操作，目前是空方法
                afterNodeRemoval(node);
                return node;
            }
        }
        return null;
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {
        java.util.HashMap.Node<K,V>[] tab;
        modCount++;
        if ((tab = table) != null && size > 0) {
            size = 0;
            for (int i = 0; i < tab.length; ++i)
                //清理，只需要把桶中的元素赋值为null，其他的就交给垃圾回收器了
                tab[i] = null;
        }
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value
     */
    public boolean containsValue(Object value) {
        java.util.HashMap.Node<K,V>[] tab; V v;
        if ((tab = table) != null && size > 0) {
            for (int i = 0; i < tab.length; ++i) {
                //因为红黑树结构也保留双向链表的结构，所以可以直接通过next迭代查找
                for (java.util.HashMap.Node<K,V> e = tab[i]; e != null; e = e.next) {
                    if ((v = e.value) == value ||
                            (value != null && value.equals(v)))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
     * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
     * operations.
     *
     * @return a set view of the keys contained in this map
     */
    public Set<K> keySet() {
        Set<K> ks = keySet;
        if (ks == null) {
            ks = new java.util.HashMap.KeySet();
            keySet = ks;
        }
        return ks;
    }

    final class KeySet extends AbstractSet<K> {
        public final int size()                 { return size; }
        public final void clear()               { java.util.HashMap.this.clear(); }
        public final Iterator<K> iterator()     { return new java.util.HashMap.KeyIterator(); }
        public final boolean contains(Object o) { return containsKey(o); }
        public final boolean remove(Object key) {
            return removeNode(hash(key), key, null, false, true) != null;
        }
        public final Spliterator<K> spliterator() {
            return new java.util.HashMap.KeySpliterator<>(java.util.HashMap.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super K> action) {
            java.util.HashMap.Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (java.util.HashMap.Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e.key);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own <tt>remove</tt> operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
     * support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a view of the values contained in this map
     */
    public Collection<V> values() {
        Collection<V> vs = values;
        if (vs == null) {
            vs = new java.util.HashMap.Values();
            values = vs;
        }
        return vs;
    }

    final class Values extends AbstractCollection<V> {
        public final int size()                 { return size; }
        public final void clear()               { java.util.HashMap.this.clear(); }
        public final Iterator<V> iterator()     { return new java.util.HashMap.ValueIterator(); }
        public final boolean contains(Object o) { return containsValue(o); }
        public final Spliterator<V> spliterator() {
            return new java.util.HashMap.ValueSpliterator<>(java.util.HashMap.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super V> action) {
            java.util.HashMap.Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (java.util.HashMap.Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e.value);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation, or through the
     * <tt>setValue</tt> operation on a map entry returned by the
     * iterator) the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
     * <tt>clear</tt> operations.  It does not support the
     * <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a set view of the mappings contained in this map
     */
    public Set<Map.Entry<K,V>> entrySet() {
        Set<Map.Entry<K,V>> es;
        return (es = entrySet) == null ? (entrySet = new java.util.HashMap.EntrySet()) : es;
    }

    final class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        public final int size()                 { return size; }
        public final void clear()               { java.util.HashMap.this.clear(); }
        public final Iterator<Map.Entry<K,V>> iterator() {
            return new java.util.HashMap.EntryIterator();
        }
        public final boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>) o;
            Object key = e.getKey();
            java.util.HashMap.Node<K,V> candidate = getNode(hash(key), key);
            return candidate != null && candidate.equals(e);
        }
        public final boolean remove(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>) o;
                Object key = e.getKey();
                Object value = e.getValue();
                return removeNode(hash(key), key, value, true, true) != null;
            }
            return false;
        }
        public final Spliterator<Map.Entry<K,V>> spliterator() {
            return new java.util.HashMap.EntrySpliterator<>(java.util.HashMap.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super Map.Entry<K,V>> action) {
            java.util.HashMap.Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (java.util.HashMap.Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    // Overrides of JDK8 Map extension methods

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        java.util.HashMap.Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? defaultValue : e.value;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return putVal(hash(key), key, value, true, true);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return removeNode(hash(key), key, value, true, true) != null;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        java.util.HashMap.Node<K,V> e; V v;
        if ((e = getNode(hash(key), key)) != null &&
                ((v = e.value) == oldValue || (v != null && v.equals(oldValue)))) {
            e.value = newValue;
            afterNodeAccess(e);
            return true;
        }
        return false;
    }

    @Override
    public V replace(K key, V value) {
        java.util.HashMap.Node<K,V> e;
        if ((e = getNode(hash(key), key)) != null) {
            V oldValue = e.value;
            e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
        return null;
    }

    @Override
    public V computeIfAbsent(K key,
                             Function<? super K, ? extends V> mappingFunction) {
        if (mappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        java.util.HashMap.Node<K,V>[] tab; java.util.HashMap.Node<K,V> first; int n, i;
        int binCount = 0;
        java.util.HashMap.TreeNode<K,V> t = null;
        java.util.HashMap.Node<K,V> old = null;
        if (size > threshold || (tab = table) == null ||
                (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof java.util.HashMap.TreeNode)
                old = (t = (java.util.HashMap.TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                java.util.HashMap.Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
            V oldValue;
            if (old != null && (oldValue = old.value) != null) {
                afterNodeAccess(old);
                return oldValue;
            }
        }
        V v = mappingFunction.apply(key);
        if (v == null) {
            return null;
        } else if (old != null) {
            old.value = v;
            afterNodeAccess(old);
            return v;
        }
        else if (t != null)
            t.putTreeVal(this, tab, hash, key, v);
        else {
            tab[i] = newNode(hash, key, v, first);
            if (binCount >= TREEIFY_THRESHOLD - 1)
                treeifyBin(tab, hash);
        }
        ++modCount;
        ++size;
        afterNodeInsertion(true);
        return v;
    }

    public V computeIfPresent(K key,
                              BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null)
            throw new NullPointerException();
        java.util.HashMap.Node<K,V> e; V oldValue;
        int hash = hash(key);
        if ((e = getNode(hash, key)) != null &&
                (oldValue = e.value) != null) {
            V v = remappingFunction.apply(key, oldValue);
            if (v != null) {
                e.value = v;
                afterNodeAccess(e);
                return v;
            }
            else
                removeNode(hash, key, null, false, true);
        }
        return null;
    }

    @Override
    public V compute(K key,
                     BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        java.util.HashMap.Node<K,V>[] tab; java.util.HashMap.Node<K,V> first; int n, i;
        int binCount = 0;
        java.util.HashMap.TreeNode<K,V> t = null;
        java.util.HashMap.Node<K,V> old = null;
        if (size > threshold || (tab = table) == null ||
                (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof java.util.HashMap.TreeNode)
                old = (t = (java.util.HashMap.TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                java.util.HashMap.Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
        }
        V oldValue = (old == null) ? null : old.value;
        V v = remappingFunction.apply(key, oldValue);
        if (old != null) {
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            }
            else
                removeNode(hash, key, null, false, true);
        }
        else if (v != null) {
            if (t != null)
                t.putTreeVal(this, tab, hash, key, v);
            else {
                tab[i] = newNode(hash, key, v, first);
                if (binCount >= TREEIFY_THRESHOLD - 1)
                    treeifyBin(tab, hash);
            }
            ++modCount;
            ++size;
            afterNodeInsertion(true);
        }
        return v;
    }

    @Override
    public V merge(K key, V value,
                   BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (value == null)
            throw new NullPointerException();
        if (remappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        java.util.HashMap.Node<K,V>[] tab; java.util.HashMap.Node<K,V> first; int n, i;
        int binCount = 0;
        java.util.HashMap.TreeNode<K,V> t = null;
        java.util.HashMap.Node<K,V> old = null;
        if (size > threshold || (tab = table) == null ||
                (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof java.util.HashMap.TreeNode)
                old = (t = (java.util.HashMap.TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                java.util.HashMap.Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
        }
        if (old != null) {
            V v;
            if (old.value != null)
                v = remappingFunction.apply(old.value, value);
            else
                v = value;
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            }
            else
                removeNode(hash, key, null, false, true);
            return v;
        }
        if (value != null) {
            if (t != null)
                t.putTreeVal(this, tab, hash, key, value);
            else {
                tab[i] = newNode(hash, key, value, first);
                if (binCount >= TREEIFY_THRESHOLD - 1)
                    treeifyBin(tab, hash);
            }
            ++modCount;
            ++size;
            afterNodeInsertion(true);
        }
        return value;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        java.util.HashMap.Node<K,V>[] tab;
        if (action == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (java.util.HashMap.Node<K,V> e = tab[i]; e != null; e = e.next)
                    action.accept(e.key, e.value);
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        java.util.HashMap.Node<K,V>[] tab;
        if (function == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (java.util.HashMap.Node<K,V> e = tab[i]; e != null; e = e.next) {
                    e.value = function.apply(e.key, e.value);
                }
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    /* ------------------------------------------------------------ */
    // Cloning and serialization

    /**
     * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        java.util.HashMap<K,V> result;
        try {
            result = (java.util.HashMap<K,V>)super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
        result.reinitialize();
        result.putMapEntries(this, false);
        return result;
    }

    // These methods are also used when serializing HashSets
    final float loadFactor() { return loadFactor; }
    final int capacity() {
        return (table != null) ? table.length :
                (threshold > 0) ? threshold :
                        DEFAULT_INITIAL_CAPACITY;
    }

    /**
     * Save the state of the <tt>HashMap</tt> instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <i>capacity</i> of the HashMap (the length of the
     *             bucket array) is emitted (int), followed by the
     *             <i>size</i> (an int, the number of key-value
     *             mappings), followed by the key (Object) and value (Object)
     *             for each key-value mapping.  The key-value mappings are
     *             emitted in no particular order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws IOException {
        int buckets = capacity();
        // Write out the threshold, loadfactor, and any hidden stuff
        s.defaultWriteObject();
        s.writeInt(buckets);
        s.writeInt(size);
        internalWriteEntries(s);
    }

    /**
     * Reconstitute the {@code HashMap} instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject(java.io.ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        // Read in the threshold (ignored), loadfactor, and any hidden stuff
        s.defaultReadObject();
        reinitialize();
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new InvalidObjectException("Illegal load factor: " +
                    loadFactor);
        s.readInt();                // Read and ignore number of buckets
        int mappings = s.readInt(); // Read number of mappings (size)
        if (mappings < 0)
            throw new InvalidObjectException("Illegal mappings count: " +
                    mappings);
        else if (mappings > 0) { // (if zero, use defaults)
            // Size the table using given load factor only if within
            // range of 0.25...4.0
            float lf = Math.min(Math.max(0.25f, loadFactor), 4.0f);
            float fc = (float)mappings / lf + 1.0f;
            int cap = ((fc < DEFAULT_INITIAL_CAPACITY) ?
                    DEFAULT_INITIAL_CAPACITY :
                    (fc >= MAXIMUM_CAPACITY) ?
                            MAXIMUM_CAPACITY :
                            tableSizeFor((int)fc));
            float ft = (float)cap * lf;
            threshold = ((cap < MAXIMUM_CAPACITY && ft < MAXIMUM_CAPACITY) ?
                    (int)ft : Integer.MAX_VALUE);

            // Check Map.Entry[].class since it's the nearest public type to
            // what we're actually creating.
            SharedSecrets.getJavaOISAccess().checkArray(s, Map.Entry[].class, cap);
            @SuppressWarnings({"rawtypes","unchecked"})
            java.util.HashMap.Node<K,V>[] tab = (java.util.HashMap.Node<K,V>[])new java.util.HashMap.Node[cap];
            table = tab;

            // Read the keys and values, and put the mappings in the HashMap
            for (int i = 0; i < mappings; i++) {
                @SuppressWarnings("unchecked")
                K key = (K) s.readObject();
                @SuppressWarnings("unchecked")
                V value = (V) s.readObject();
                putVal(hash(key), key, value, false, false);
            }
        }
    }

    /* ------------------------------------------------------------ */
    // iterators

    abstract class HashIterator {
        java.util.HashMap.Node<K,V> next;        // next entry to return
        java.util.HashMap.Node<K,V> current;     // current entry
        int expectedModCount;  // for fast-fail
        int index;             // current slot

        HashIterator() {
            expectedModCount = modCount;
            java.util.HashMap.Node<K,V>[] t = table;
            current = next = null;
            index = 0;
            if (t != null && size > 0) { // advance to first entry
                do {} while (index < t.length && (next = t[index++]) == null);
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        final java.util.HashMap.Node<K,V> nextNode() {
            java.util.HashMap.Node<K,V>[] t;
            java.util.HashMap.Node<K,V> e = next;
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (e == null)
                throw new NoSuchElementException();
            if ((next = (current = e).next) == null && (t = table) != null) {
                do {} while (index < t.length && (next = t[index++]) == null);
            }
            return e;
        }

        public final void remove() {
            java.util.HashMap.Node<K,V> p = current;
            if (p == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            current = null;
            K key = p.key;
            removeNode(hash(key), key, null, false, false);
            expectedModCount = modCount;
        }
    }

    final class KeyIterator extends java.util.HashMap.HashIterator
            implements Iterator<K> {
        public final K next() { return nextNode().key; }
    }

    final class ValueIterator extends java.util.HashMap.HashIterator
            implements Iterator<V> {
        public final V next() { return nextNode().value; }
    }

    final class EntryIterator extends java.util.HashMap.HashIterator
            implements Iterator<Map.Entry<K,V>> {
        public final Map.Entry<K,V> next() { return nextNode(); }
    }

    /* ------------------------------------------------------------ */
    // spliterators

    static class HashMapSpliterator<K,V> {
        final java.util.HashMap<K,V> map;
        java.util.HashMap.Node<K,V> current;          // current node
        int index;                  // current index, modified on advance/split
        int fence;                  // one past last index
        int est;                    // size estimate
        int expectedModCount;       // for comodification checks

        HashMapSpliterator(java.util.HashMap<K,V> m, int origin,
                           int fence, int est,
                           int expectedModCount) {
            this.map = m;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getFence() { // initialize fence and size on first use
            int hi;
            if ((hi = fence) < 0) {
                java.util.HashMap<K,V> m = map;
                est = m.size;
                expectedModCount = m.modCount;
                java.util.HashMap.Node<K,V>[] tab = m.table;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            return hi;
        }

        public final long estimateSize() {
            getFence(); // force init
            return (long) est;
        }
    }

    static final class KeySpliterator<K,V>
            extends java.util.HashMap.HashMapSpliterator<K,V>
            implements Spliterator<K> {
        KeySpliterator(java.util.HashMap<K,V> m, int origin, int fence, int est,
                       int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public java.util.HashMap.KeySpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                    new java.util.HashMap.KeySpliterator<>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        public void forEachRemaining(Consumer<? super K> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            java.util.HashMap<K,V> m = map;
            java.util.HashMap.Node<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                    (i = index) >= 0 && (i < (index = hi) || current != null)) {
                java.util.HashMap.Node<K,V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.key);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            java.util.HashMap.Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        K k = current.key;
                        current = current.next;
                        action.accept(k);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                    Spliterator.DISTINCT;
        }
    }

    static final class ValueSpliterator<K,V>
            extends java.util.HashMap.HashMapSpliterator<K,V>
            implements Spliterator<V> {
        ValueSpliterator(java.util.HashMap<K,V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public java.util.HashMap.ValueSpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                    new java.util.HashMap.ValueSpliterator<>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        public void forEachRemaining(Consumer<? super V> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            java.util.HashMap<K,V> m = map;
            java.util.HashMap.Node<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                    (i = index) >= 0 && (i < (index = hi) || current != null)) {
                java.util.HashMap.Node<K,V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.value);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            java.util.HashMap.Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        V v = current.value;
                        current = current.next;
                        action.accept(v);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0);
        }
    }

    static final class EntrySpliterator<K,V>
            extends java.util.HashMap.HashMapSpliterator<K,V>
            implements Spliterator<Map.Entry<K,V>> {
        EntrySpliterator(java.util.HashMap<K,V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public java.util.HashMap.EntrySpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                    new java.util.HashMap.EntrySpliterator<>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        public void forEachRemaining(Consumer<? super Map.Entry<K,V>> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            java.util.HashMap<K,V> m = map;
            java.util.HashMap.Node<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                    (i = index) >= 0 && (i < (index = hi) || current != null)) {
                java.util.HashMap.Node<K,V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super Map.Entry<K,V>> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            java.util.HashMap.Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        java.util.HashMap.Node<K,V> e = current;
                        current = current.next;
                        action.accept(e);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                    Spliterator.DISTINCT;
        }
    }

    /* ------------------------------------------------------------ */
    // LinkedHashMap support


    /*
     * The following package-protected methods are designed to be
     * overridden by LinkedHashMap, but not by any other subclass.
     * Nearly all other internal methods are also package-protected
     * but are declared final, so can be used by LinkedHashMap, view
     * classes, and HashSet.
     */

    // Create a regular (non-tree) node
    java.util.HashMap.Node<K,V> newNode(int hash, K key, V value, java.util.HashMap.Node<K,V> next) {
        return new java.util.HashMap.Node<>(hash, key, value, next);
    }

    // For conversion from TreeNodes to plain nodes
    java.util.HashMap.Node<K,V> replacementNode(java.util.HashMap.Node<K,V> p, java.util.HashMap.Node<K,V> next) {
        return new java.util.HashMap.Node<>(p.hash, p.key, p.value, next);
    }

    // Create a tree bin node
    java.util.HashMap.TreeNode<K,V> newTreeNode(int hash, K key, V value, java.util.HashMap.Node<K,V> next) {
        return new java.util.HashMap.TreeNode<>(hash, key, value, next);
    }

    // For treeifyBin
    java.util.HashMap.TreeNode<K,V> replacementTreeNode(java.util.HashMap.Node<K,V> p, java.util.HashMap.Node<K,V> next) {
        return new java.util.HashMap.TreeNode<>(p.hash, p.key, p.value, next);
    }

    /**
     * Reset to initial default state.  Called by clone and readObject.
     */
    void reinitialize() {
        table = null;
        entrySet = null;
        keySet = null;
        values = null;
        modCount = 0;
        threshold = 0;
        size = 0;
    }

    // Callbacks to allow LinkedHashMap post-actions
    void afterNodeAccess(java.util.HashMap.Node<K,V> p) { }
    void afterNodeInsertion(boolean evict) { }
    void afterNodeRemoval(java.util.HashMap.Node<K,V> p) { }

    // Called only from writeObject, to ensure compatible ordering.
    void internalWriteEntries(java.io.ObjectOutputStream s) throws IOException {
        java.util.HashMap.Node<K,V>[] tab;
        if (size > 0 && (tab = table) != null) {
            for (int i = 0; i < tab.length; ++i) {
                for (java.util.HashMap.Node<K,V> e = tab[i]; e != null; e = e.next) {
                    s.writeObject(e.key);
                    s.writeObject(e.value);
                }
            }
        }
    }


    /**
     * Entry for Tree bins. Extends LinkedHashMap.Entry (which in turn
     * extends Node) so can be used as extension of either regular or
     * linked node.
     */
    static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
        java.util.HashMap.TreeNode<K,V> parent;  // 父节点
        java.util.HashMap.TreeNode<K,V> left; //左子树
        java.util.HashMap.TreeNode<K,V> right;//右子树
        java.util.HashMap.TreeNode<K,V> prev;    // 删除后需要取消链接
        boolean red;//颜色属性
        TreeNode(int hash, K key, V val, java.util.HashMap.Node<K,V> next) {
            super(hash, key, val, next);
        }

        /**
         * 返回当前红黑树的根节点，父节点为null的节点就为根节点
         */
        final java.util.HashMap.TreeNode<K,V> root() {
            for (java.util.HashMap.TreeNode<K,V> r = this, p;;) {
                if ((p = r.parent) == null)
                    return r;
                r = p;
            }
        }

        /**
         * Ensures that the given root is the first node of its bin.
         * 为什么还要保留双向链表结构呢？单向链表不行吗？
         * 在resize()拆分红黑树的时候，需要通过双向链表结构快速遍历树节点的所有元素。
         * 在调整成红黑树之后，还需要把树的根节点放到对应的桶位置，并且把根结点调整为双向
         * 链表的头节点，若是没有双向链表结构，那么把根节点变更为双向链表的头节点就不能在O(1)的时间复杂度完成。
         */
        static <K,V> void moveRootToFront(java.util.HashMap.Node<K,V>[] tab, java.util.HashMap.TreeNode<K,V> root) {
            int n;
            if (root != null && tab != null && (n = tab.length) > 0) {
                int index = (n - 1) & root.hash;
                java.util.HashMap.TreeNode<K,V> first = (java.util.HashMap.TreeNode<K,V>)tab[index];
                if (root != first) {
                    java.util.HashMap.Node<K,V> rn;
                    tab[index] = root;
                    java.util.HashMap.TreeNode<K,V> rp = root.prev;
                    if ((rn = root.next) != null)
                        ((java.util.HashMap.TreeNode<K,V>)rn).prev = rp;
                    if (rp != null)
                        rp.next = rn;
                    if (first != null)
                        first.prev = root;
                    root.next = first;
                    root.prev = null;
                }
                assert checkInvariants(root);
            }
        }

        /**
         * Finds the node starting at root p with the given hash and key.
         * The kc argument caches comparableClassFor(key) upon first use
         * comparing keys.
         */
        final java.util.HashMap.TreeNode<K,V> find(int h, Object k, Class<?> kc) {
            java.util.HashMap.TreeNode<K,V> p = this;
            do {
                int ph, dir; K pk;
                java.util.HashMap.TreeNode<K,V> pl = p.left, pr = p.right, q;
                if ((ph = p.hash) > h)
                    p = pl;
                else if (ph < h)
                    p = pr;
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    return p;
                else if (pl == null)
                    p = pr;
                else if (pr == null)
                    p = pl;
                else if ((kc != null ||
                        (kc = comparableClassFor(k)) != null) &&
                        (dir = compareComparables(kc, k, pk)) != 0)
                    p = (dir < 0) ? pl : pr;
                else if ((q = pr.find(h, k, kc)) != null)
                    return q;
                else
                    p = pl;
            } while (p != null);
            return null;
        }

        /**
         * Calls find for root node.
         */
        final java.util.HashMap.TreeNode<K,V> getTreeNode(int h, Object k) {
            return ((parent != null) ? root() : this).find(h, k, null);
        }

        /**
         * Tie-breaking utility for ordering insertions when equal
         * hashCodes and non-comparable. We don't require a total
         * order, just a consistent insertion rule to maintain
         * equivalence across rebalancings. Tie-breaking further than
         * necessary simplifies testing a bit.
         */
        static int tieBreakOrder(Object a, Object b) {
            int d;
            if (a == null || b == null ||
                    (d = a.getClass().getName().
                            compareTo(b.getClass().getName())) == 0)
                d = (System.identityHashCode(a) <= System.identityHashCode(b) ?
                        -1 : 1);
            return d;
        }

        /**
         * Forms tree of the nodes linked from this node.
         * @return root of tree
         */
        final void treeify(java.util.HashMap.Node<K,V>[] tab) {
            java.util.HashMap.TreeNode<K,V> root = null;
            //this 就是头节点
            for (java.util.HashMap.TreeNode<K,V> x = this, next; x != null; x = next) {
                next = (java.util.HashMap.TreeNode<K,V>)x.next;
                x.left = x.right = null;
                //红黑树的头节点是黑色的。
                if (root == null) {
                    x.parent = null;
                    x.red = false;
                    root = x;
                }
                else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    for (java.util.HashMap.TreeNode<K,V> p = root;;) {
                        int dir, ph;
                        K pk = p.key;
                        if ((ph = p.hash) > h)
                            dir = -1;
                        else if (ph < h)
                            dir = 1;
                        else if ((kc == null &&
                                (kc = comparableClassFor(k)) == null) ||
                                (dir = compareComparables(kc, k, pk)) == 0)
                            dir = tieBreakOrder(k, pk);

                        java.util.HashMap.TreeNode<K,V> xp = p;
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            x.parent = xp;
                            if (dir <= 0)
                                xp.left = x;
                            else
                                xp.right = x;
                            root = balanceInsertion(root, x);
                            break;
                        }
                    }
                }
            }
            moveRootToFront(tab, root);
        }

        /**
         * Returns a list of non-TreeNodes replacing those linked from
         * this node.
         */
        final java.util.HashMap.Node<K,V> untreeify(java.util.HashMap<K,V> map) {
            java.util.HashMap.Node<K,V> hd = null, tl = null;
            for (java.util.HashMap.Node<K,V> q = this; q != null; q = q.next) {
                java.util.HashMap.Node<K,V> p = map.replacementNode(q, null);
                if (tl == null)
                    hd = p;
                else
                    tl.next = p;
                tl = p;
            }
            return hd;
        }

        /**
         * 以下的逻辑就是：把新加的节点插入红黑树中，是按顺序加入的。并且在插入树节点的地方
         * 也会加入一个双向链表中(直接与父节点形成双向链表)
         */
        final java.util.HashMap.TreeNode<K,V> putTreeVal(java.util.HashMap<K,V> map, java.util.HashMap.Node<K,V>[] tab,
                                                         int h, K k, V v) {
            Class<?> kc = null;
            boolean searched = false;
            java.util.HashMap.TreeNode<K,V> root = (parent != null) ? root() : this;
            for (java.util.HashMap.TreeNode<K,V> p = root;;) {
                //dir 保存tree节点之间key的大小比较
                int dir, ph; K pk;
                if ((ph = p.hash) > h)
                    dir = -1;
                else if (ph < h)
                    dir = 1;
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    return p;
                else if ((kc == null &&
                        (kc = comparableClassFor(k)) == null) ||
                        (dir = compareComparables(kc, k, pk)) == 0) {
                    if (!searched) {
                        java.util.HashMap.TreeNode<K,V> q, ch;
                        searched = true;
                        if (((ch = p.left) != null &&
                                (q = ch.find(h, k, kc)) != null) ||
                                ((ch = p.right) != null &&
                                        (q = ch.find(h, k, kc)) != null))
                            return q;
                    }
                    //若是key不能比较，那么直接采用默认的比较方式
                    dir = tieBreakOrder(k, pk);
                }

                java.util.HashMap.TreeNode<K,V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    java.util.HashMap.Node<K,V> xpn = xp.next;
                    java.util.HashMap.TreeNode<K,V> x = map.newTreeNode(h, k, v, xpn);
                    if (dir <= 0)
                        xp.left = x;
                    else
                        xp.right = x;
                    xp.next = x;
                    x.parent = x.prev = xp;
                    if (xpn != null)
                        ((java.util.HashMap.TreeNode<K,V>)xpn).prev = x;
                    moveRootToFront(tab, balanceInsertion(root, x));
                    return null;
                }
            }
        }

        /**
         * Removes the given node, that must be present before this call.
         * This is messier than typical red-black deletion code because we
         * cannot swap the contents of an interior node with a leaf
         * successor that is pinned by "next" pointers that are accessible
         * independently during traversal. So instead we swap the tree
         * linkages. If the current tree appears to have too few nodes,
         * the bin is converted back to a plain bin. (The test triggers
         * somewhere between 2 and 6 nodes, depending on tree structure).
         */
        final void removeTreeNode(java.util.HashMap<K,V> map, java.util.HashMap.Node<K,V>[] tab,
                                  boolean movable) {
            int n;
            if (tab == null || (n = tab.length) == 0)
                return;
            int index = (n - 1) & hash;
            java.util.HashMap.TreeNode<K,V> first = (java.util.HashMap.TreeNode<K,V>)tab[index], root = first, rl;
            java.util.HashMap.TreeNode<K,V> succ = (java.util.HashMap.TreeNode<K,V>)next, pred = prev;
            if (pred == null)
                tab[index] = first = succ;
            else
                pred.next = succ;
            if (succ != null)
                succ.prev = pred;
            if (first == null)
                return;
            if (root.parent != null)
                root = root.root();
            if (root == null || root.right == null ||
                    (rl = root.left) == null || rl.left == null) {
                tab[index] = first.untreeify(map);  // too small
                return;
            }
            java.util.HashMap.TreeNode<K,V> p = this, pl = left, pr = right, replacement;
            if (pl != null && pr != null) {
                java.util.HashMap.TreeNode<K,V> s = pr, sl;
                while ((sl = s.left) != null) // find successor
                    s = sl;
                boolean c = s.red; s.red = p.red; p.red = c; // swap colors
                java.util.HashMap.TreeNode<K,V> sr = s.right;
                java.util.HashMap.TreeNode<K,V> pp = p.parent;
                if (s == pr) { // p was s's direct parent
                    p.parent = s;
                    s.right = p;
                }
                else {
                    java.util.HashMap.TreeNode<K,V> sp = s.parent;
                    if ((p.parent = sp) != null) {
                        if (s == sp.left)
                            sp.left = p;
                        else
                            sp.right = p;
                    }
                    if ((s.right = pr) != null)
                        pr.parent = s;
                }
                p.left = null;
                if ((p.right = sr) != null)
                    sr.parent = p;
                if ((s.left = pl) != null)
                    pl.parent = s;
                if ((s.parent = pp) == null)
                    root = s;
                else if (p == pp.left)
                    pp.left = s;
                else
                    pp.right = s;
                if (sr != null)
                    replacement = sr;
                else
                    replacement = p;
            }
            else if (pl != null)
                replacement = pl;
            else if (pr != null)
                replacement = pr;
            else
                replacement = p;
            if (replacement != p) {
                java.util.HashMap.TreeNode<K,V> pp = replacement.parent = p.parent;
                if (pp == null)
                    root = replacement;
                else if (p == pp.left)
                    pp.left = replacement;
                else
                    pp.right = replacement;
                p.left = p.right = p.parent = null;
            }

            java.util.HashMap.TreeNode<K,V> r = p.red ? root : balanceDeletion(root, replacement);

            if (replacement == p) {  // detach
                java.util.HashMap.TreeNode<K,V> pp = p.parent;
                p.parent = null;
                if (pp != null) {
                    if (p == pp.left)
                        pp.left = null;
                    else if (p == pp.right)
                        pp.right = null;
                }
            }
            if (movable)
                moveRootToFront(tab, r);
        }

        /**
         *
         */
        final void split(java.util.HashMap<K,V> map, java.util.HashMap.Node<K,V>[] tab, int index, int bit) {
            //当前Tree节点对象
            java.util.HashMap.TreeNode<K,V> b = this;
            //重新连接到低和高的名单，维持秩序
            // Relink into lo and hi lists, preserving order
            java.util.HashMap.TreeNode<K,V> loHead = null, loTail = null;
            java.util.HashMap.TreeNode<K,V> hiHead = null, hiTail = null;
            int lc = 0, hc = 0;
            //由于在链超过8个数据时，先传化成双向链表->然后再转化为红黑树，此时双向链表的指向关系还是保留的。
            //此处就是通过双向链表的结构遍历红黑树。并且红黑树中添加节点的时候，也会维护双向链表指向关系。
            for (java.util.HashMap.TreeNode<K,V> e = b, next; e != null; e = next) {
                next = (java.util.HashMap.TreeNode<K,V>)e.next;
                e.next = null;
                //这里的e.hash & bit(旧hashMap的oldCap【length】)是什么用意呢？
                //JDK1.7中，resize时，index取得时，全部采用重新hash的方式进行了。JDK1.8对这个进行了改善。
                //
                //以前要确定index的时候用的是(e.hash & oldCap-1)，是取模取余，而这里用到的是(e.hash & oldCap)，它有两种结果，一个是0，一个是oldCap，
                //
                //比如oldCap=8,hash是3，11，19，27时，(e.hash & oldCap)的结果是0，8，0，8，这样3，19组成新的链表，index为3；而11，27组成新的链表，新分配的index为3+8；
                //
                //JDK1.7中重写hash是(e.hash & newCap-1)，也就是3，11，19，27对16取余，也是3，11，3，11，和上面的结果一样，但是index为3的链表是19，3，index为3+8的链表是
                //
                //27，11，也就是说1.7中经过resize后数据的顺序变成了倒叙，而1.8没有改变顺序。
                if ((e.hash & bit) == 0) {
                    if ((e.prev = loTail) == null)
                        loHead = e;
                    else
                        loTail.next = e;
                    loTail = e;
                    ++lc;
                }
                else {
                    if ((e.prev = hiTail) == null)
                        hiHead = e;
                    else
                        hiTail.next = e;
                    hiTail = e;
                    ++hc;
                }
            }
            //红黑树在重新rehash后，会分为两个链表，一个是在原位置的链表(低位链表)，一个是原位置+oldCap 位置的链表(高位链表)
            // 此时的树结构还是保留，重新构建双向链表结构。

            //为什么要把红黑树拆解呢？
            //①、红黑树的结构是为了解决长链表的线性查找效率为问题。
            //②、当rehash之后，链表长度不长的情况下，红黑树带来的查找性能不明显，反而加大了红黑树插入节点的复杂性。
            //③、一个红黑树节点占用的内存比一般的节点要大。红黑树节点多了四个引用(父节点、左孩子、右孩子、双向链表中的前向引用)
            if (loHead != null) {
                //低位链表 长度小于6，则需要转化为链表式结构
                if (lc <= UNTREEIFY_THRESHOLD)
                    //把红黑树拆解
                    tab[index] = loHead.untreeify(map);
                else {
                    //否则仍然保留红黑树结构
                    tab[index] = loHead;
                    if (hiHead != null) // (else is already treeified)
                        //用低位链表重新构建新的红黑树
                        loHead.treeify(tab);
                }
            }
            if (hiHead != null) {
                if (hc <= UNTREEIFY_THRESHOLD)
                    tab[index + bit] = hiHead.untreeify(map);
                else {
                    tab[index + bit] = hiHead;
                    if (loHead != null)
                        hiHead.treeify(tab);
                }
            }
        }

        /* ------------------------------------------------------------ */
        // Red-black tree methods, all adapted from CLR
        //红黑树调整的 左旋操作
        static <K,V> java.util.HashMap.TreeNode<K,V> rotateLeft(java.util.HashMap.TreeNode<K,V> root,
                                                                java.util.HashMap.TreeNode<K,V> p) {
            java.util.HashMap.TreeNode<K,V> r, pp, rl;
            if (p != null && (r = p.right) != null) {
                if ((rl = p.right = r.left) != null)
                    rl.parent = p;
                if ((pp = r.parent = p.parent) == null)
                    (root = r).red = false;
                else if (pp.left == p)
                    pp.left = r;
                else
                    pp.right = r;
                r.left = p;
                p.parent = r;
            }
            return root;
        }
        //红黑树调整的 右旋操作
        static <K,V> java.util.HashMap.TreeNode<K,V> rotateRight(java.util.HashMap.TreeNode<K,V> root,
                                                                 java.util.HashMap.TreeNode<K,V> p) {
            java.util.HashMap.TreeNode<K,V> l, pp, lr;
            if (p != null && (l = p.left) != null) {
                if ((lr = p.left = l.right) != null)
                    lr.parent = p;
                if ((pp = l.parent = p.parent) == null)
                    (root = l).red = false;
                else if (pp.right == p)
                    pp.right = l;
                else
                    pp.left = l;
                l.right = p;
                p.parent = l;
            }
            return root;
        }
        //添加节点之后调整红黑树平衡
        static <K,V> java.util.HashMap.TreeNode<K,V> balanceInsertion(java.util.HashMap.TreeNode<K,V> root,
                                                                      java.util.HashMap.TreeNode<K,V> x) {
            x.red = true;
            for (java.util.HashMap.TreeNode<K,V> xp, xpp, xppl, xppr;;) {
                if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                }
                else if (!xp.red || (xpp = xp.parent) == null)
                    return root;
                if (xp == (xppl = xpp.left)) {
                    if ((xppr = xpp.right) != null && xppr.red) {
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    }
                    else {
                        if (x == xp.right) {
                            root = rotateLeft(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                }
                else {
                    if (xppl != null && xppl.red) {
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    }
                    else {
                        if (x == xp.left) {
                            root = rotateRight(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }
        //删除节点之后调整红黑树平衡
        static <K,V> java.util.HashMap.TreeNode<K,V> balanceDeletion(java.util.HashMap.TreeNode<K,V> root,
                                                                     java.util.HashMap.TreeNode<K,V> x) {
            for (java.util.HashMap.TreeNode<K,V> xp, xpl, xpr;;)  {
                if (x == null || x == root)
                    return root;
                else if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                }
                else if (x.red) {
                    x.red = false;
                    return root;
                }
                else if ((xpl = xp.left) == x) {
                    if ((xpr = xp.right) != null && xpr.red) {
                        xpr.red = false;
                        xp.red = true;
                        root = rotateLeft(root, xp);
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }
                    if (xpr == null)
                        x = xp;
                    else {
                        java.util.HashMap.TreeNode<K,V> sl = xpr.left, sr = xpr.right;
                        if ((sr == null || !sr.red) &&
                                (sl == null || !sl.red)) {
                            xpr.red = true;
                            x = xp;
                        }
                        else {
                            if (sr == null || !sr.red) {
                                if (sl != null)
                                    sl.red = false;
                                xpr.red = true;
                                root = rotateRight(root, xpr);
                                xpr = (xp = x.parent) == null ?
                                        null : xp.right;
                            }
                            if (xpr != null) {
                                xpr.red = (xp == null) ? false : xp.red;
                                if ((sr = xpr.right) != null)
                                    sr.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateLeft(root, xp);
                            }
                            x = root;
                        }
                    }
                }
                else { // symmetric
                    if (xpl != null && xpl.red) {
                        xpl.red = false;
                        xp.red = true;
                        root = rotateRight(root, xp);
                        xpl = (xp = x.parent) == null ? null : xp.left;
                    }
                    if (xpl == null)
                        x = xp;
                    else {
                        java.util.HashMap.TreeNode<K,V> sl = xpl.left, sr = xpl.right;
                        if ((sl == null || !sl.red) &&
                                (sr == null || !sr.red)) {
                            xpl.red = true;
                            x = xp;
                        }
                        else {
                            if (sl == null || !sl.red) {
                                if (sr != null)
                                    sr.red = false;
                                xpl.red = true;
                                root = rotateLeft(root, xpl);
                                xpl = (xp = x.parent) == null ?
                                        null : xp.left;
                            }
                            if (xpl != null) {
                                xpl.red = (xp == null) ? false : xp.red;
                                if ((sl = xpl.left) != null)
                                    sl.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateRight(root, xp);
                            }
                            x = root;
                        }
                    }
                }
            }
        }

        /**
         * Recursive invariant check
         */
        static <K,V> boolean checkInvariants(java.util.HashMap.TreeNode<K,V> t) {
            java.util.HashMap.TreeNode<K,V> tp = t.parent, tl = t.left, tr = t.right,
                    tb = t.prev, tn = (java.util.HashMap.TreeNode<K,V>)t.next;
            if (tb != null && tb.next != t)
                return false;
            if (tn != null && tn.prev != t)
                return false;
            if (tp != null && t != tp.left && t != tp.right)
                return false;
            if (tl != null && (tl.parent != t || tl.hash > t.hash))
                return false;
            if (tr != null && (tr.parent != t || tr.hash < t.hash))
                return false;
            if (t.red && tl != null && tl.red && tr != null && tr.red)
                return false;
            if (tl != null && !checkInvariants(tl))
                return false;
            if (tr != null && !checkInvariants(tr))
                return false;
            return true;
        }
    }
}