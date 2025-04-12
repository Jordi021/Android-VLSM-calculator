package com.androidvlsm.utils;

import com.androidvlsm.utils.IPAddress;

public class Subred {
    private String nombre;
    private int size;
    private IPAddress direccion;

    // Constructor
    public Subred(String nombre, int size, IPAddress direccion) {
        this.nombre = nombre;
        this.size = size;
        this.direccion = direccion;
    }

    public Subred(String nombre, int size) {
        this.nombre = nombre;
        this.size = size;
        // Si la dirección es null, usa la IP predeterminada "0.0.0.0" con prefijo 1
        this.direccion = new IPAddress("0.0.0.0", 1);
    }

    // Getter y Setter para 'nombre'
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    // Getter y Setter para 'size'
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        if (size > 0) {
            this.size = size;
        }
    }

    // Getter y Setter para 'direccion'
    public IPAddress getDireccion() {
        return direccion;
    }

    public void setDireccion(IPAddress direccion) {
        this.direccion = direccion;
    }

    // Método toString para representar el objeto en forma de cadena
    @Override
    public String toString() {
        return "Nombre: " + this.nombre + "\n" +
               "Tamaño: " + this.size + "\n" +
               this.direccion.toString();
    }
}
