package com.androidvlsm.utils;

public class Converters {

    // Convierte un número binario a decimal
    public static int binarioADecimal(String numero) {
        int decimal = 0;
        for (int i = 0, elevado = numero.length() - 1; i < numero.length(); i++, elevado--) {
            char digito = numero.charAt(i);
            if (digito == '1') {
                decimal += (int) Math.pow(2, elevado);
            }
        }
        return decimal;
    }

    // Convierte un número decimal a binario
    public static String decimalABinario(int numero) {
        StringBuilder resultado = new StringBuilder();
        if (numero == 0) {
            return "00000000"; // Retorna "00000000" si el número es 0
        }

        while (numero > 0) {
            int resto = numero % 2;
            resultado.insert(0, resto); // Añadir el resto al principio
            numero = Math.floorDiv(numero, 2); // Realiza la división entera
        }

        // Asegura que siempre haya 8 bits
        int cerosFaltantes = 8 - resultado.length();
        for (int i = 0; i < cerosFaltantes; i++) {
            resultado.insert(0, "0"); // Añade ceros al principio
        }
        return resultado.toString();
    }

    // Calcula los bits necesarios para un número de hosts
    public static int bitsNecesarios(int numHosts) {
        int bitsNecesarios = 0;
        while (Math.pow(2, bitsNecesarios) < numHosts + 2) {
            bitsNecesarios++;
        }
        return bitsNecesarios;
    }

    // Convierte una dirección IP en formato decimal a binario
    public static String direccionIPaBinario(String ip) {
        String[] octetos = ip.split("\\.");
        StringBuilder binario = new StringBuilder();
        for (String octeto : octetos) {
            binario.append(decimalABinario(Integer.parseInt(octeto)));
        }
        return binario.toString();
    }

    // Convierte una dirección IP en formato binario a decimal
    public static String binarioADireccionIP(String binario) {
        StringBuilder direccionIP = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            String octetoBinario = binario.substring(i * 8, (i + 1) * 8);
            int octetoDecimal = binarioADecimal(octetoBinario);
            direccionIP.append(octetoDecimal);
            if (i < 3) {
                direccionIP.append("."); // Añadir punto entre octetos
            }
        }
        return direccionIP.toString();
    }

    // Modifica un carácter de una cadena en una posición dada
    public static String modificarCaracter(String cadena, int indice, String nuevoCaracter) {
        if (indice < 0 || indice >= cadena.length()) {
            System.err.println("Índice fuera de rango");
            return cadena;
        }
        return cadena.substring(0, indice) + nuevoCaracter + cadena.substring(indice + 1);
    }
}

