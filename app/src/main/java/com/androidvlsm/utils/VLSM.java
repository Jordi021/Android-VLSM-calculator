package com.androidvlsm.utils;

import java.util.ArrayList;
import java.util.List;
import com.androidvlsm.utils.IPAddress;
import com.androidvlsm.utils.Subred;
import com.androidvlsm.utils.Converters;
import com.androidvlsm.utils.NodoABB;

public class VLSM {
    private IPAddress ipAddress;
    private String ipPadre;
    private int prefijo;
    private List<Subred> subredes;
    private NodoABB nodoRaiz;

    // Constructor
    public VLSM(IPAddress ipAddress) {
        this.ipAddress = ipAddress;
        this.ipPadre = Converters.direccionIPaBinario(ipAddress.getIp());
        this.prefijo = ipAddress.getPrefijo();
        this.nodoRaiz = null;
        this.subredes = new ArrayList<>();   
    }

    // Getter para ipPadre
    public String getIpPadre() {
        return ipPadre;
    }

    // Método para agregar una subred
    public List<Subred> agregarSubred(String nombre, int size) {
        Subred nuevaSubred = new Subred(nombre, size);
        this.subredes.add(nuevaSubred);
        ordenarSubredes();
        return this.subredes;
    }

    // Método privado para ordenar las subredes por tamaño y nombre
    private void ordenarSubredes() {
        subredes.sort((a, b) -> {
            if (a.getSize() != b.getSize()) {
                return b.getSize() - a.getSize();
            } else {
                return a.getNombre().compareTo(b.getNombre());
            }
        });
    }

    // Getter para subredes
    public List<Subred> getSubredes() {
        vlsm();
        return subredes;
    }

    // Método privado para realizar el proceso de VLSM
    private void vlsm() {
        nodoRaiz = new NodoABB(new Object[] { ipPadre, prefijo });
        for (Subred subRedActual : subredes) {
            calcularSubRed(nodoRaiz, subRedActual);
        }
    }

    // Método privado para calcular cada subred
    private void calcularSubRed(NodoABB nodo, Subred subRedActual) {
        if (!subRedActual.getDireccion().getIp().equals("0.0.0.0") || subRedActual.getDireccion().getPrefijo() > 1) {
            return;
        }

        int prefijoActual = nodo.getArbolPrefijo();
        if (32 - Converters.bitsNecesarios(subRedActual.getSize()) <= prefijoActual) {
            nodo.setMarked(true);
            subRedActual.setDireccion(new IPAddress(Converters.binarioADireccionIP(nodo.getArbolIp()), prefijoActual));
            return;
        }

        NodoABB newNodo;
        if (nodo.getHijoIzq() != null) {
            Boolean izquierdo = buscarIp(
                Converters.binarioADireccionIP(nodo.getHijoIzq().getArbolIp()),
                nodo.getHijoIzq().getArbolPrefijo()
            );
            if (izquierdo || nodo.getHijoIzq().isMarked() == true) {
                if (nodo.getHijoDer() != null) {
                    Boolean derecho = buscarIp(
                        Converters.binarioADireccionIP(nodo.getHijoDer().getArbolIp()),
                        nodo.getHijoDer().getArbolPrefijo()
                    );
                    boolean marcados = nodo.getHijoIzq().isMarked() && nodo.getHijoDer().isMarked();
                    if (marcados) {
                        nodo.setMarked(true);
                        calcularSubRed(nodo.getNodoAnterior(), subRedActual);
                    }
                    if (derecho) {
                        nodo.setMarked(true);
                        nodo = nodo.getNodoAnterior();
                        prefijoActual = nodo.getArbolPrefijo();
                        if (nodo.getHijoDer() != null) {
                            calcularSubRed(nodo.getHijoDer(), subRedActual);
                        } else {
                            String nuevaIp = Converters.modificarCaracter(nodo.getArbolIp(), prefijoActual, "1");
                            newNodo = new NodoABB(new Object[] { nuevaIp, prefijoActual + 1 });
                            nodo.setHijoDer(newNodo);
                            newNodo.setNodoAnterior(nodo);
                            calcularSubRed(newNodo, subRedActual);
                        }
                    } else {
                        calcularSubRed(nodo.getHijoDer(), subRedActual);
                    }
                } else {
                    String nuevaIp = Converters.modificarCaracter(nodo.getArbolIp(), prefijoActual, "1");
                    newNodo = new NodoABB(new Object[] { nuevaIp, prefijoActual + 1 });
                    nodo.setHijoDer(newNodo);
                    newNodo.setNodoAnterior(nodo);
                    calcularSubRed(newNodo, subRedActual);
                }
            } else {
                calcularSubRed(nodo.getHijoIzq(), subRedActual);
            }
        } else {
            newNodo = new NodoABB(new Object[] { nodo.getArbolIp(), prefijoActual + 1 });
            nodo.setHijoIzq(newNodo);
            newNodo.setNodoAnterior(nodo);
            calcularSubRed(newNodo, subRedActual);
        }
    }

    // Método privado para buscar una IP en las subredes
    private boolean buscarIp(String ip, int prefijo) {
        for (Subred subred : subredes) {
            if (subred.getDireccion().getIp().equals(ip) && subred.getDireccion().getPrefijo() == prefijo) {
                return true;
            }
        }
        return false;
    }
//     private boolean buscarIp(String ip, int prefijo) {
//     for (int i = 0; i < subredes.size(); i++) {
//         String ipArray = subredes.get(i).getDireccion().getIp();  // Accediendo a la IP
//         int prefijoArray = subredes.get(i).getDireccion().getPrefijo();  // Accediendo al prefijo
        
//         if (ipArray.equals(ip) && prefijoArray == prefijo) {
//             return true;
//         }
//     }
//     return false;
// }

//     #buscarIp(ip, prefijo) {
//         for (let i = 0; i < this.#subredes.length; i++) {
//             let ipArray, prefijoArray;
//             ipArray = this.#subredes[i].direccion.ip;
//             prefijoArray = this.#subredes[i].direccion.prefijo;
//             if (ipArray == ip && prefijoArray == prefijo) {
//                 return true;
//             }
//         }
//         return false;
//     }

}
