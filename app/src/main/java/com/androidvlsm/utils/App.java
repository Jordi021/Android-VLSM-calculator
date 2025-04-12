package com.androidvlsm.utils;
import com.androidvlsm.utils.*;

public class App {
    public static void main(String[] args) {
        IPAddress ip = new IPAddress("172.30.4.0", 22);
        VLSM vlsm = new VLSM(ip);

        // Agregar subredes
        vlsm.agregarSubred("Subred A", 60);
        vlsm.agregarSubred("Subred B", 10);
        vlsm.agregarSubred("Subred C", 250);
        vlsm.agregarSubred("Subred D", 100);
        vlsm.agregarSubred("Subred E", 2);
        vlsm.agregarSubred("Subred F", 2);
        vlsm.agregarSubred("Subred G", 2);

        // Mostrar las subredes 
        for (Subred subred : vlsm.getSubredes()) {
            System.out.println(subred.toString());
            System.out.println("===================================");
        }
    }
}
