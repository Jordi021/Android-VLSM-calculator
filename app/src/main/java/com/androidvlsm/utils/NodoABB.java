package com.androidvlsm.utils;

public class NodoABB {
    private Object[] data;
    private NodoABB hijoIzq;
    private NodoABB hijoDer;
    private NodoABB padre;
    private boolean marked;

    // Constructor
    public NodoABB(Object[] data) {
        this.data = data;
        this.hijoIzq = null;
        this.hijoDer = null;
        this.padre = null;
        this.marked = false;
    }

    // Getter y Setter para 'data'
    public Object[] getData() {
        return data;
    }

    public void setData(Object[] data) {
        this.data = data;
    }

    // Getter y Setter para 'marked'
    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    // Getter y Setter para 'hijoIzq'
    public NodoABB getHijoIzq() {
        return hijoIzq;
    }

    public void setHijoIzq(NodoABB hijoIzq) {
        this.hijoIzq = hijoIzq;
    }

    // Getter y Setter para 'hijoDer'
    public NodoABB getHijoDer() {
        return hijoDer;
    }

    public void setHijoDer(NodoABB hijoDer) {
        this.hijoDer = hijoDer;
    }

    // Getter para 'arbolIp'
    public String getArbolIp() {
        return (String) data[0];
    }

    // Getter para 'arbolPrefijo'
    public int getArbolPrefijo() {
        return (int) data[1];
    }

    // Setter y Getter para 'padre' (nodoAnterior)
    public void setNodoAnterior(NodoABB nodo) {
        this.padre = nodo;
    }

    public NodoABB getNodoAnterior() {
        return this.padre;
    }
}