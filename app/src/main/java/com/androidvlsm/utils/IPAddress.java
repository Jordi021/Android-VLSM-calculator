package com.androidvlsm.utils;

import com.androidvlsm.utils.Converters;
public class IPAddress {
    private String ip;
    private int prefijo;

    // Constructor
    public IPAddress(String ip, int prefijo) {
        this.ip = ip;
        this.prefijo = prefijo;
    }

    // Getter y Setter para la IP
    public String getIp() {
        return ip;
    }

    public boolean setIp(String nuevaIP) {
        String[] octetos = nuevaIP.split("\\.");
        if (octetos.length != 4) {
            System.err.println("La dirección IP debe contener cuatro octetos.");
            return false;
        }

        for (String octeto : octetos) {
            int valor = Integer.parseInt(octeto);
            if (valor < 0 || valor > 255) {
                System.err.println("Cada octeto de la dirección IP debe estar en el rango válido (0 - 255).");
                return false;
            }
        }

        this.ip = nuevaIP;
        return true;
    }

    // Getter y Setter para el prefijo
    public int getPrefijo() {
        return prefijo;
    }

    public void setPrefijo(int prefijo) {
        if (prefijo > 0) {
            this.prefijo = prefijo;
        }
    }

    // Calcular la máscara de red basada en el prefijo
    public String getMascara() {
        return calcularMascaraDeRed(this.prefijo);
    }

    // Dirección de primer host
    public String getFirstDir() {
        String[] octetos = this.ip.split("\\.");
        for (int i = 0; i < octetos.length; i++) {
            octetos[i] = String.valueOf(Integer.parseInt(octetos[i]));
        }
        octetos[3] = String.valueOf(Integer.parseInt(octetos[3]) + 1);
        return String.join(".", octetos);
    }

    // Dirección de último host
    public String getLastDir() {
        String[] octetos = getBroadcast().split("\\.");
        for (int i = 0; i < octetos.length; i++) {
            octetos[i] = String.valueOf(Integer.parseInt(octetos[i]));
        }
        octetos[3] = String.valueOf(Integer.parseInt(octetos[3]) - 1);
        return String.join(".", octetos);
    }

    // Dirección de broadcast
    public String getBroadcast() {
        String ipBinario = Converters.direccionIPaBinario(this.ip);
        StringBuilder sb = new StringBuilder(ipBinario);
        for (int i = this.prefijo; i < ipBinario.length(); i++) {
            sb.setCharAt(i, '1');
        }
        return Converters.binarioADireccionIP(sb.toString());
    }

    // Método privado para calcular la máscara de red
    private String calcularMascaraDeRed(int prefijo) {
        if (prefijo < 1 || prefijo > 30) {
            System.err.println("Prefijo de red no válido");
            return "";
        }

        int[] mascara = new int[4];
        for (int i = 0; i < 4; i++) {
            if (prefijo >= 8) {
                mascara[i] = 255;
                prefijo -= 8;
            } else {
                mascara[i] = 256 - (int) Math.pow(2, 8 - prefijo);
                prefijo = 0;
            }
        }

        return String.join(".", String.valueOf(mascara[0]), String.valueOf(mascara[1]), 
                String.valueOf(mascara[2]), String.valueOf(mascara[3]));
    }

    // Método para representar la clase como cadena
    @Override
    public String toString() {
        return "Dirección: " + this.ip + "\n" +
                "Prefijo: " + this.prefijo + "\n" +
                "Máscara: " + this.getMascara() + "\n" +
                "Primer Host: " + this.getFirstDir() + "\n" +
                "Ultimo Host: " + this.getLastDir() + "\n" +
                "Broadcast: " + this.getBroadcast();
    }
}

