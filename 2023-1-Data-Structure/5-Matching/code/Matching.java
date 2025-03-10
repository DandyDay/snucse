import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Matching {
    static AVLTreeHashTable hashTable;

    public static void main(String[] args) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        hashTable = new AVLTreeHashTable();
        while (true) {
            try {
                String input = br.readLine();
                if (input.compareTo("QUIT") == 0)
                    break;

                command(input);
            } catch (IOException e) {
                System.out.println("입력이 잘못되었습니다. 오류 : " + e.toString());
            }
        }
    }

    private static void command(String input) throws IOException {
        // Check input length
        if (input.length() < 3)
            throw new IOException("input is short");
        //get command type and argument
        char cmd = input.charAt(0);
        String argument = input.substring(2);
        switch (cmd) {
            case '<':       // File input
                hashTable = new AVLTreeHashTable(); // Reset table when reading file
                hashTable.readFile(argument);       // Insert file data to data structure
                break ;
            case '@':
                hashTable.printAVLTreeAtIndex(argument);    //Print tree preorder in index
                break ;
            case '?':
                if (argument.length() < 6)      // ? gets substring longer than 6
                    throw new IOException("too short argument");
                TupleList tupleList = hashTable.searchPattern(argument);
                if (tupleList != null)
                    System.out.println(tupleList);
                else
                    System.out.println("(0, 0)");
                break ;
            case '/':
                if (argument.length() != 6)     // / gets substring whose length is 6
                    throw new IOException("argument length have to be 6");
                System.out.println(hashTable.deletePattern(argument));
                break ;
            case '+':
                hashTable.addString(argument);
                break ;

            /* For Debug
             * "# [idx]"
             * Prints AVLTree in HashTable[idx]
             * "# 100"
             * Prints all HashTable
             */
            case '#':
                System.out.println(hashTable.rawString);
                hashTable.printHashTable(argument);
                break ;
            default:
                throw new IOException("command not found");
        }
    }
}

/* AVLTreeHashTable class that manages substrings
 *
 * rawString contains strings to deal with deletion command
 *
 * public methods to execute commands,
 * private method is used in public methods
 */
class AVLTreeHashTable extends HashTable<LinkedListAVLTree> {

    ArrayList<String> rawString;        // To manage strings when deletion

    public AVLTreeHashTable() {
        this.table = new LinkedListAVLTree[DEFAULT_CAPACITY];
        this.numItems = 0;
        this.rawString = new ArrayList<>();
        for (int i = 0; i < 100; i++)
            this.table[i] = null;
    }

    private int hash(String key) {
        int sum = 0;
        for (char c : key.toCharArray()) {
            sum += c;
        }
        return (sum % table.length);
    }

    // readfile() Code by ChatGPT
    public void readFile(String filename) throws IOException{
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Process each line of the file
                this.rawString.add(line);
                this.insertSubstring(line, this.rawString.size() - 1);
            }
        } catch (IOException e) {
            throw e;
        }
    }

    public void printAVLTreeAtIndex(String indexString) {
        int index = Integer.parseInt(indexString);
        LinkedListAVLTree tree = this.table[index];
        if (tree == null)
            System.out.println("EMPTY");
        else
            System.out.println(tree);
    }

    public TupleList searchPattern(String pattern) {
        TupleList tupleList = this.searchList(pattern.substring(0, 6));
        if (tupleList == null)
            return null;
        else {
            TupleList tempList = new TupleList();
            for (Tuple t : tupleList) {
                tempList.add(t);
            }
            for (int idx = 1; idx < pattern.length() - 5; idx = idx + 11 < pattern.length() ? idx + 5 : idx + 1) {       // To reduce comparison
                String substring = pattern.substring(idx, idx + 6);
                TupleList substringTupleList = this.searchList(substring);
                if (substringTupleList == null)
                    return (null);
                for (Tuple t : tempList) {
                    boolean Contains = false;
                    for (Tuple substringT : substringTupleList) {
                        if (t.i == substringT.i && t.j + idx == substringT.j) {
                            Contains = true;
                            break;
                        }
                    }
                    if (!Contains) {
                        tempList.removeItem(t);
                    }
                }
                if (tempList.isEmpty()) {
                    return null;
                }
            }
            if (!tempList.isEmpty()) {
                return tempList;
            }
        }
        return null;
    }

    public int deletePattern (String pattern) {
        TupleList tupleList = this.searchList(pattern);
        ArrayList<Integer> rehashingStrings= new ArrayList<>();
        if (tupleList == null)
            return 0;
        else {
            TupleList deleteIndex = new TupleList();
            for (Tuple t : tupleList) {
                for (int i = 0; i < 6; i++) {
                    Tuple tmp = new Tuple(t.i, t.j + i);
                    if (!deleteIndex.isContained(tmp))
                        deleteIndex.add(tmp);
                }
            }

            int deletedNode = tupleList.size();
            for (Tuple t : tupleList) {
                if (!rehashingStrings.contains(t.i)) {
                    this.deleteSubstring(t.i - 1);
                    rehashingStrings.add(t.i);
                }
            }

            LinkedListAVLTree tree = this.search(pattern);
            if (tree != null)
                tree.deleteNode(pattern);
            for (int i : rehashingStrings) {
                String original = this.rawString.get(i - 1);
                StringBuilder ret = new StringBuilder();
                for (int idx = 0; idx < original.length(); idx++) {
                    if (!deleteIndex.isContained(new Tuple(i, idx + 1)))
                        ret.append(original.charAt(idx));
                }
                this.rawString.set(i - 1, ret.toString());
                this.insertSubstring(rawString.get(i - 1), i - 1);
            }
            return (deletedNode);
        }
    }

    private void deleteSubstring(int i) {
        String string = this.rawString.get(i);
        for (int j = 0; j < string.length() - 5; j++) {
            this.deleteTuple(string.substring(j, j + 6), new Tuple(i + 1, j + 1));
        }
    }

    private void deleteTuple(String substring, Tuple t) {
        LinkedListAVLTree tree = this.search(substring);
        if (tree != null) {
            TupleList tupleList = this.searchList(substring);
            if (tupleList != null) {
                tupleList.removeItem(t);
                if (tupleList.isEmpty()) {
                    tree.deleteNode(substring);
                    if (tree.isNIL(tree.root))
                        this.table[this.hash(substring)] = null;
                }
            }
        }
    }

    public void addString(String argument) {
        this.rawString.add(argument);
        this.insertSubstring(argument, this.rawString.size() - 1);
    }

    private void insertSubstring(String string, int i) {
        for (int j = 0; j < string.length() - 5; j++) {
            this.insertTuple(string.substring(j, j + 6), new Tuple(i + 1, j + 1));
        }
    }

    private void insertTuple(String key, Tuple t) {
        LinkedListAVLTree substringTree = search(key);
        if (substringTree == null) {
            table[this.hash(key)] = new LinkedListAVLTree();
            table[this.hash(key)].insertTuple(key, t);
        } else {
            substringTree.insertTuple(key, t);
        }
    }

    private LinkedListAVLTree search(String key) {
        return (this.table[this.hash(key)]);
    }

    private TupleList searchList(String key) {
        LinkedListAVLTree tree = this.search(key);
        if (tree == null)
            return null;
        else {
            AVLNode<String, TupleList> node = tree.search(key);
            if (tree.isNIL(node))
                return null;
            else
                return (tree.search(key).content);
        }
    }

    // For Debug ONLY
    public void printHashTable(String argument) throws IOException {
        try {
            int index = Integer.parseInt(argument);
            if (index == 100) {
                for (int i = 0; i < table.length; i++) {
                    if (table[i] != null) {
                        System.out.println("Table[" + i + "]");
                        AVLTreePrinter.visualizeTree(table[i].root);
                    }
                }
            }
            else if (index >= 0 && index <= 99) {
                System.out.println("Table[" + index + "]");
                if (table[index] == null)
                    System.out.println("EMPTY");
                else
                    AVLTreePrinter.visualizeTree(table[index].root);
            }
            else
                throw new IOException("Argument not appropriate");
        } catch (NumberFormatException e) {
            throw new IOException("Numeric argument required");
        } catch (IOException e) {
            throw e;
        }
    }
}

// Edited HashTable class code by Lecture Note
class HashTable<E> {
    protected E[] table;
    int numItems;

    static final int DEFAULT_CAPACITY = 100;

    public HashTable() {
        this.table = (E[]) new Object[DEFAULT_CAPACITY];
        this.numItems = 0;
        for (int i = 0; i < 100; i++)
            this.table[i] = null;
    }

    public HashTable(int capacity) {
        this.table = (E[]) new Object[capacity];
        this.numItems = 0;
    }

    protected int hash(E x) {
        return (Math.abs(x.hashCode() % table.length));
    }

    public void insert(E item) {
        table[hash(item)] = item;
    }

    public E search(E item) {
        return (table[hash(item)]);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < table.length; i++) {
            ret.append("table[").append(i).append("]\n");
            ret.append(table[i].toString()).append("\n");
        }
        return ret.toString();
    }
}

// LinkedListAVLTree class that is element of HashTable
class LinkedListAVLTree extends AVLTree<String, TupleList> {
    public void insertTuple(String key, Tuple t) {
        AVLNode<String, TupleList> Node = search(key);
        if (this.isNIL(Node)) {
            TupleList newList = new TupleList();
            newList.add(t);
            this.insert(key, newList);
        } else {
            Node.content.add(t);
        }
    }

    public void deleteNode(String key) {
        this.delete(key);
    }
}

// Edited AVLNode class code by Lecture Note
class AVLNode<KEY, E> {
    public KEY key;
    public E content;
    public AVLNode<KEY, E> left, right;
    public int height;

    public AVLNode(KEY key, AVLNode<KEY, E> leftChild, AVLNode<KEY, E> rightChild, int h) {
        this.key = key;
        this.content = (E) new Object();
        this.left = leftChild;
        this.right = rightChild;
        this.height = 1;
    }

    public AVLNode(KEY key, E content, AVLNode<KEY, E> leftChild, AVLNode<KEY, E> rightChild, int h) {
        this.key = key;
        this.content = content;
        this.left = leftChild;
        this.right = rightChild;
        this.height = 1;
    }

    public String toString() {
        String ret = this.key.toString();
        if (this.left.height > 1)
            ret += " " + this.left.toString();
        if (this.right.height > 1)
            ret += " " + this.right.toString();
        return ret;
    }
}

// Edited AVLTree class code by Lecture Note
class AVLTree<KEY extends Comparable<? super KEY>, E> {
    protected AVLNode<KEY, E> root;

    // generic AVLNode can't be static
    private AVLNode<KEY, E> NIL = new AVLNode<>(null, null, null, 0);

    public AVLTree() {
        root = NIL;
    }

    public AVLNode<KEY, E> search(KEY key) {
        return searchItem(root, key);
    }

    public AVLNode<KEY, E> searchItem(AVLNode<KEY, E> tNode, KEY key) {
        if (tNode == NIL)
            return NIL;
        else if (key.compareTo(tNode.key) == 0)
            return tNode;
        else if (key.compareTo(tNode.key) < 0)
            return searchItem(tNode.left, key);
        else
            return searchItem(tNode.right, key);
    }

    public void insert(KEY key, E content) {
        root = insertItem(root, key, content);
    }

    private AVLNode<KEY, E> insertItem(AVLNode<KEY, E> tNode, KEY key, E content) {
        if (tNode == NIL) {
            tNode = new AVLNode<>(key, content, NIL, NIL, 1);
        } else if (key.compareTo(tNode.key) < 0) {
            tNode.left = insertItem(tNode.left, key, content);
            tNode.left = checkBalance(tNode.left);
        } else if (key.compareTo(tNode.key) > 0) {
            tNode.right = insertItem(tNode.right, key, content);
            tNode.right = checkBalance(tNode.right);
        }
        tNode = checkBalance(tNode);
        return tNode;
    }

    private final int LL = 1, LR = 2, RR = 3, RL = 4, NO_NEED = 0, ILLEGAL = -1;

    private int needBalance(AVLNode<KEY, E> t) {
        int type = ILLEGAL;
        if (t.left.height + 2 <= t.right.height) {
            if (t.right.left.height <= t.right.right.height)
                type = RR;
            else
                type = RL;
        } else if (t.left.height >= t.right.height + 2) {
            if (t.left.left.height >= t.left.right.height)
                type = LL;
            else
                type = LR;
        } else
            type = NO_NEED;
        return type;
    }

    private AVLNode<KEY, E> balanceAVL(AVLNode<KEY, E> tNode, int type) {
        AVLNode<KEY, E> returnNode = NIL;
        switch (type) {
            case LL:
                returnNode = rightRotate(tNode);
                break;
            case LR:
                tNode.left = leftRotate(tNode.left);
                returnNode = rightRotate(tNode);
                break;
            case RR:
                returnNode = leftRotate(tNode);
                break;
            case RL:
                tNode.right = rightRotate(tNode.right);
                returnNode = leftRotate(tNode);
                break;
        }
        return returnNode;
    }

    private AVLNode<KEY, E> leftRotate(AVLNode<KEY, E> tNode) {
        AVLNode<KEY, E> RChild = tNode.right;
        AVLNode<KEY, E> RLChild = RChild.left;
        RChild.left = tNode;
        tNode.right = RLChild;
        tNode.height = 1 + Math.max(tNode.left.height, tNode.right.height);
        RChild.height = 1 + Math.max(RChild.left.height, RChild.right.height);
        return RChild;
    }

    private AVLNode<KEY, E> rightRotate(AVLNode<KEY, E> tNode) {
        AVLNode<KEY, E> LChild = tNode.left;
        AVLNode<KEY, E> LRChild = LChild.right;
        LChild.right = tNode;
        tNode.left = LRChild;
        tNode.height = 1 + Math.max(tNode.left.height, tNode.right.height);
        LChild.height = 1 + Math.max(LChild.left.height, LChild.right.height);
        return LChild;
    }

    public void delete(KEY key) {
        root = deleteItem(root, key);
    }

    private AVLNode<KEY, E> deleteItem(AVLNode<KEY, E> tNode, KEY key) {
        if (tNode == NIL) return NIL;
        else {
            if (key.compareTo(tNode.key) == 0) {
                tNode = deleteNode(tNode);
            } else if (key.compareTo(tNode.key) < 0) {
                tNode.left = deleteItem(tNode.left, key);
                tNode = checkBalance(tNode);
            } else {
                tNode.right = deleteItem(tNode.right, key);
                tNode = checkBalance(tNode);
            }
            return tNode;
        }
    }

    private AVLNode<KEY, E> deleteNode(AVLNode<KEY, E> tNode) {
        if ((tNode.left == NIL) && (tNode.right == NIL))
            return NIL;
        else if (tNode.left == NIL)
            return tNode.right;
        else if (tNode.right == NIL)
            return tNode.left;
        else {
            returnPair rPair = deleteMinItem(tNode.right);
            tNode.key = rPair.key;
            tNode.right = rPair.node;
            tNode = checkBalance(tNode);
            return tNode;
        }
    }

    private returnPair deleteMinItem(AVLNode<KEY, E> tNode) {
        int type;
        if (tNode.left == NIL) {
            return new returnPair(tNode.key, tNode.right);
        } else {
            returnPair rPair = deleteMinItem(tNode.left);
            tNode.left = rPair.node;
            tNode = checkBalance(tNode);
            rPair.node = tNode;
            return rPair;
        }
    }

    private AVLNode<KEY, E> checkBalance(AVLNode<KEY, E> tNode) {
        tNode.height = 1 + Math.max(tNode.right.height, tNode.left.height);
        int type = needBalance(tNode);
        if (type != NO_NEED)
            tNode = balanceAVL(tNode, type);
        return (tNode);
    }

    private class returnPair {
        private KEY key;
        private AVLNode<KEY, E> node;
        private returnPair(KEY key, AVLNode<KEY, E> node) {
            this.key = key;
            this.node = node;
        }
    }

    public boolean isNIL(AVLNode<KEY, E> node) {
        return (node == NIL);
    }

    public String toString() {
        if (this.root == NIL)
            return ("EMPTY");
        else {
            return (this.root.toString());
        }
    }
}

// TupleList class that is node of AVLTree
class TupleList extends MyLinkedList<Tuple> {
    public String toString() {
        String ret = "";
        ListNode<Tuple> curr = this.head;
        while (curr.getNext() != null) {
            ret += curr.getNext().getItem();
            if (curr.getNext().getNext() != null)
                ret += " ";
            curr = curr.getNext();
        }
        return ret;
    }

    public boolean isContained(Tuple o) {
        for (Tuple t : this) {
            if (t.compareTo(o) == 0) {
                return true;
            }
        }
        return false;
    }
}

// Edited LinkedList class code by HW2
class MyLinkedList<T extends Comparable<T>> implements Iterable<T> {
    ListNode<T> head;
    int numItems;

    public MyLinkedList() {
        head = new ListNode<T>(null);
    }

    public final Iterator<T> iterator() {
        return new MyLinkedListIterator<T>(this);
    }

    public boolean isEmpty() {
        return head.getNext() == null;
    }

    public int size() {
        return numItems;
    }

    public T first() {
        return head.getNext().getItem();
    }

    public void add(T item) {
        ListNode<T> last = head;
        while (last.getNext() != null && last.getNext().getItem().compareTo(item) < 0) {
            last = last.getNext();
        }
        last.insertNext(item);
        numItems += 1;
    }

    public void removeItem(T item) {
        ListNode<T> curr = this.head;
        while (curr.getNext() != null && curr.getNext().getItem().compareTo(item) != 0) {
            curr = curr.getNext();
        }
        if (curr.getNext() != null)
            curr.removeNext();
        numItems -= 1;
    }

    public void removeAll() {
        head.setNext(null);
    }
}

// Edited LinkedListIterator class code by HW2
class MyLinkedListIterator<T extends Comparable<T>> implements Iterator<T> {
    private MyLinkedList<T> list;
    private ListNode<T> curr;
    private ListNode<T> prev;

    public MyLinkedListIterator(MyLinkedList<T> list) {
        this.list = list;
        this.curr = list.head;
        this.prev = null;
    }

    @Override
    public boolean hasNext() {
        return curr.getNext() != null;
    }

    @Override
    public T next() {
        if (!hasNext())
            throw new NoSuchElementException();

        prev = curr;
        curr = curr.getNext();

        return curr.getItem();
    }

    @Override
    public void remove() {
        if (prev == null)
            throw new IllegalStateException("next() should be called first");
        if (curr == null)
            throw new NoSuchElementException();
        prev.removeNext();
        list.numItems -= 1;
        curr = prev;
        prev = null;
    }
}

// LinkedList Node class code by HW2
class ListNode<T> {
    private T item;
    private ListNode<T> next;

    public ListNode(T obj) {
        this.item = obj;
        this.next = null;
    }

    public ListNode(T obj, ListNode<T> next) {
        this.item = obj;
        this.next = next;
    }

    public final T getItem() {
        return item;
    }

    public final void setItem(T item) {
        this.item = item;
    }

    public final void setNext(ListNode<T> next) {
        this.next = next;
    }

    public ListNode<T> getNext() {
        return this.next;
    }

    public final void insertNext(T obj) {
        ListNode<T> newNode = new ListNode<>(obj);
        newNode.next = this.next;
        this.next = newNode;
    }

    public final void removeNext() {
        this.next = this.next.next;
    }
}

// Tuple class to manage coordinate of substring
class Tuple implements Comparable<Tuple> {
    public int i;
    public int j;

    public Tuple(int i, int j) {
        this.i = i;
        this.j = j;
    }

    public String toString() {
        String s;

        s = "(" + i + ", " + j + ")";
        return (s);
    }

    @Override
    public int compareTo(Tuple o) {
        if (this.i != o.i)
            return (this.i - o.i);
        else if (this.j != o.j)
            return (this.j - o.j);
        else
            return 0;
    }
}

// AVLTreePrinter code by ChatGPT
class AVLTreePrinter {
    public static void visualizeTree(AVLNode<?, ?> root) {
        visualizeTree(root, "", false);
    }

    private static void visualizeTree(AVLNode<?, ?> node, String prefix, boolean isLeft) {
        if (node.height == 1) {
            return;
        }

        visualizeTree(node.right, prefix + (isLeft ? "│   " : "    "), false);

        System.out.print(prefix);
        System.out.print(isLeft ? "└── " : "┌── ");
        System.out.print(node.key);
        System.out.println(node.content.toString());

        visualizeTree(node.left, prefix + (isLeft ? "    " : "│   "), true);
    }
}
