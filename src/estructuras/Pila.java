package estructuras;

import java.util.ArrayList;
import java.util.List;

public class Pila<T> {

    private Nodo<T> cima;
    private int tamano;

    public Pila() {
        this.cima = null;
        this.tamano = 0;
    }

    public void apilar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.setSiguiente(cima);
        cima = nuevo;
        tamano++;
    }

    public T desapilar() {
        if (estaVacia()) {
            return null;
        }
        T dato = cima.getDato();
        cima = cima.getSiguiente();
        tamano--;
        return dato;
    }

    public T verCima() {
        if (estaVacia()) {
            return null;
        }
        return cima.getDato();
    }

    public boolean estaVacia() {
        return cima == null;
    }

    public int tamano() {
        return tamano;
    }

    public void vaciar() {
        cima = null;
        tamano = 0;
    }

    public List<T> recorrer() {
        List<T> elementos = new ArrayList<>();
        Nodo<T> actual = cima;
        while (actual != null) {
            elementos.add(actual.getDato());
            actual = actual.getSiguiente();
        }
        return elementos;
    }
}
