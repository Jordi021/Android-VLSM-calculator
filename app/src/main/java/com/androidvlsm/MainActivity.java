package com.androidvlsm;

import android.os.Bundle;
import android.text.Html; // Importar para formato HTML
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity; // Importar para Gravity
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import android.view.inputmethod.InputMethodManager;

// Imports de AndroidX
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // Para colores

// Imports de utils
import com.androidvlsm.utils.Converters; // Asegúrate que esté importado
import com.androidvlsm.utils.IPAddress;
import com.androidvlsm.utils.Subred;
import com.androidvlsm.utils.VLSM;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.net.InetAddress; // Para validación de IP
import java.net.UnknownHostException; // Para validación de IP
import java.nio.ByteBuffer; // Para calcular IP de red

public class MainActivity extends AppCompatActivity {

    // Referencias a Vistas de la UI
    private LinearLayout subnetContainer;
    private EditText etIp;
    private EditText etPrefix;
    private EditText etNumSubnet;
    private Button btnChangeSubnets;
    private Button btnCalculate;
    private LinearLayout resultsDisplayContainer; // Contenedor para resultados
    private TextView tvResultsHeader; // Título de resultados
    private Button btnNewCalculation; // Botón para nuevo cálculo (se creará dinámicamente)

    // Constantes
    private final int DEFAULT_SUBNETS = 4; // Usado como fallback o para lógica interna
    private final int MIN_SUBNETS = 1;
    private final int MAX_SUBNETS = 50; // Límite razonable para evitar sobrecarga de UI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Asegúrate que este layout exista y contenga los IDs

        // --- Referencias a Vistas ---
        subnetContainer = findViewById(R.id.subnetContainer);
        etIp = findViewById(R.id.etIp);
        etPrefix = findViewById(R.id.etPrefix);
        etNumSubnet = findViewById(R.id.numSubnet);
        btnChangeSubnets = findViewById(R.id.btnChang); // Asegúrate que el ID sea 'btnChang' en tu XML
        btnCalculate = findViewById(R.id.btnCalc);     // Asegúrate que el ID sea 'btnCalc' en tu XML
        resultsDisplayContainer = findViewById(R.id.resultsDisplayContainer);
        tvResultsHeader = findViewById(R.id.textViewResultsHeader); // Asegúrate que exista en tu XML

        // --- Estado Inicial ---
        if (savedInstanceState == null) {
            resetCalculatorState(false); // Usa el método de reseteo para el estado inicial
        }

        // --- Listeners ---
        btnChangeSubnets.setOnClickListener(v -> handleChangeButtonClick());
        btnCalculate.setOnClickListener(v -> handleCalculateButtonClick());

        // Inicialmente el header de resultados está oculto
        tvResultsHeader.setVisibility(View.GONE);
    }

    /**
     * Resetea la interfaz a su estado inicial, limpia todos los campos
     * y oculta el teclado.
     * @param showToast Indica si mostrar un Toast de confirmación.
     */
    private void resetCalculatorState(boolean showToast) {
        // 1. Limpiar campos principales y errores
        etIp.setText("");
        etPrefix.setText("");
        etIp.setError(null);
        etPrefix.setError(null);
        etNumSubnet.setError(null);

        // 2. Limpiar resultados anteriores y ocultar header
        resultsDisplayContainer.removeAllViews();
        tvResultsHeader.setVisibility(View.GONE);
        // También ocultar el botón de nuevo cálculo si ya existe y es visible
        if (btnNewCalculation != null) {
            btnNewCalculation.setVisibility(View.GONE);
        }


        // 3. Resetear número de subredes en el EditText
        etNumSubnet.setText(String.valueOf(DEFAULT_SUBNETS));

        // 4. ***Importante: Limpiar explícitamente el contenedor de subredes***
        subnetContainer.removeAllViews();

        // 5. Recrear las vistas de subred por defecto
        // Llamar a updateSubnetViews AHORA creará las nuevas vistas limpias
        // porque el contenedor está vacío.
        updateSubnetViews(DEFAULT_SUBNETS);

        // 6. Ocultar el teclado
        hideKeyboard();

        // 7. Poner el foco en el primer campo (opcional)
        etIp.requestFocus();

        // 8. Mostrar Toast si se solicitó
        if (showToast) {
            Toast.makeText(this, "Formulario reiniciado", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Oculta el teclado virtual si está visible.
     */
    private void hideKeyboard() {
        View view = this.getCurrentFocus(); // Obtener la vista que tiene el foco
        if (view != null) {
            // Obtener el servicio InputMethodManager
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                // Ocultar el teclado para la vista con foco
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            view.clearFocus(); // Opcional: quitar el foco de la vista actual
        } else {
            // Si ninguna vista tiene foco, intentar ocultarlo desde la vista raíz
            view = findViewById(android.R.id.content); // Vista raíz de la actividad
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        }
    }


    /**
     * Botón "Cambiar" (Actualizar Nº Subredes).
     */
    private void handleChangeButtonClick() {
        int targetCount;
        String numSubnetStr = etNumSubnet.getText().toString().trim();

        if (numSubnetStr.isEmpty()) {
            numSubnetStr = String.valueOf(DEFAULT_SUBNETS);
            etNumSubnet.setText(numSubnetStr); // Actualizar UI si estaba vacío
        }

        try {
            targetCount = Integer.parseInt(numSubnetStr);
            if (targetCount < MIN_SUBNETS) {
                targetCount = MIN_SUBNETS;
                etNumSubnet.setText(String.valueOf(targetCount)); // Corregir en UI
                etNumSubnet.setError("Mínimo " + MIN_SUBNETS);
                Toast.makeText(this, "El número mínimo de subredes es " + MIN_SUBNETS, Toast.LENGTH_SHORT).show();
                return; // No actualizar vistas si es inválido
            } else if (targetCount > MAX_SUBNETS) {
                targetCount = MAX_SUBNETS;
                etNumSubnet.setText(String.valueOf(targetCount)); // Corregir en UI
                etNumSubnet.setError("Máximo " + MAX_SUBNETS);
                Toast.makeText(this, "El número máximo de subredes es " + MAX_SUBNETS, Toast.LENGTH_SHORT).show();
                return; // No actualizar vistas si es inválido
            } else {
                etNumSubnet.setError(null); // Limpiar error si es válido
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Número de subredes inválido", Toast.LENGTH_SHORT).show();
            etNumSubnet.setError("Número inválido");
            etNumSubnet.requestFocus();
            // No intentar obtener childCount si falla la conversión
            // targetCount = DEFAULT_SUBNETS; // Podríamos volver al default
            // etNumSubnet.setText(String.valueOf(targetCount));
            return; // Salir si el número es inválido
        }

        // Si llegamos aquí, targetCount es válido
        updateSubnetViews(targetCount);
    }

    /**
     * Sincroniza la UI para que muestre 'targetCount' pares de EditText.
     * Limpia los campos al añadir/quitar.
     */
    private void updateSubnetViews(int targetCount) {
        int currentCount = subnetContainer.getChildCount();

        // Añadir vistas si faltan
        if (targetCount > currentCount) {
            for (int i = currentCount; i < targetCount; i++) {
                addSubnetPair(i);
            }
        }
        // Quitar vistas si sobran
        else if (targetCount < currentCount) {
            for (int i = currentCount - 1; i >= targetCount; i--) {
                // Es más seguro quitar desde el final
                if (subnetContainer.getChildAt(i) != null) {
                    subnetContainer.removeViewAt(i);
                }
            }
        }
        // Si targetCount == currentCount, no hacemos nada, pero podríamos querer
        // limpiar/resetear los valores existentes si fuera necesario.
        // Por ahora, los dejamos como están si el número no cambia.

        // Asegurar que el número en el EditText coincida
        // (podría haber sido corregido por MIN/MAX)
        if (!etNumSubnet.getText().toString().equals(String.valueOf(targetCount))) {
            etNumSubnet.setText(String.valueOf(targetCount));
        }

    }

    /**
     * Crea y añade un par de EditText (Nombre, Tamaño) al contenedor.
     */
    private void addSubnetPair(int index) {
        LinearLayout pairLayout = new LinearLayout(this);
        pairLayout.setOrientation(LinearLayout.HORIZONTAL);
        LayoutParams pairParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        // Añadir margen superior solo si no es el primer elemento
        if (index > 0) {
            pairParams.setMargins(0, dpToPx(8), 0, 0); // Margen superior entre pares
        }
        pairLayout.setLayoutParams(pairParams);

        // EditText para Nombre (Net)
        EditText etNetName = new EditText(this);
        LayoutParams etNameParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 0.5f); // Peso 0.5 para nombre
        etNameParams.setMarginEnd(dpToPx(8)); // Margen a la derecha del nombre
        etNetName.setLayoutParams(etNameParams);
        char labelChar = (char) ('A' + index);
        etNetName.setHint(getString(R.string.hint_subnet_name)); // Usar string resource
        etNetName.setText(getString(R.string.subnet_label_prefix, labelChar)); // Usar string resource
        etNetName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS); // Capitalizar palabras
        etNetName.setId(View.generateViewId()); // ID único para posible estado

        // EditText para Tamaño (Size)
        EditText etNetSize = new EditText(this);
        LayoutParams etSizeParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 0.5f); // Peso 0.5 para tamaño
        etNetSize.setLayoutParams(etSizeParams);
        etNetSize.setText(""); // Empezar vacío
        etNetSize.setHint(getString(R.string.hint_subnet_size)); // Usar string resource
        etNetSize.setInputType(InputType.TYPE_CLASS_NUMBER);
        etNetSize.setId(View.generateViewId()); // ID único

        pairLayout.addView(etNetName);
        pairLayout.addView(etNetSize);
        subnetContainer.addView(pairLayout);
    }

    /**
     * Botón "Calcular".
     */
    private void handleCalculateButtonClick() {
        // --- 0. Limpiar resultados anteriores ---
        resultsDisplayContainer.removeAllViews();
        tvResultsHeader.setVisibility(View.GONE); // Ocultar título inicialmente

        // --- 1. Recoger y Validar Datos de la Interfaz ---
        String ipPadreStr = etIp.getText().toString().trim();
        String prefijoPadreStr = etPrefix.getText().toString().trim();
        int prefijoPadre;
        IPAddress redPadre;

        // --- Validaciones IP/Prefijo Padre ---
        // Validación de IP (más robusta)
        if (!isValidIpAddress(ipPadreStr)) {
            Toast.makeText(this, "Formato de IP padre inválido", Toast.LENGTH_SHORT).show();
            etIp.setError("Formato inválido (ej: 192.168.1.0)");
            etIp.requestFocus();
            return;
        } else {
            etIp.setError(null); // Limpiar error previo
        }

        // Validación de Prefijo
        if (prefijoPadreStr.isEmpty()) {
            Toast.makeText(this, "Prefijo padre requerido", Toast.LENGTH_SHORT).show();
            etPrefix.setError("Requerido");
            etPrefix.requestFocus();
            return;
        }
        try {
            prefijoPadre = Integer.parseInt(prefijoPadreStr);
            if (prefijoPadre < 1 || prefijoPadre > 30) { // VLSM típicamente hasta /30
                throw new IllegalArgumentException("Prefijo padre debe estar entre 1 y 30");
            }
            etPrefix.setError(null); // Limpiar error previo
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Prefijo padre debe ser un número", Toast.LENGTH_SHORT).show();
            etPrefix.setError("Número inválido");
            etPrefix.requestFocus();
            return;
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            etPrefix.setError(e.getMessage());
            etPrefix.requestFocus();
            return;
        }

        // Validar si la IP es una dirección de red para el prefijo dado (Recomendado)
        String calculatedNetworkIp = calculateNetworkAddress(ipPadreStr, prefijoPadre);
        if (!ipPadreStr.equals(calculatedNetworkIp)) {
            String errorMsg = String.format(Locale.getDefault(),
                    "La IP %s no es una dirección de red válida para el prefijo /%d. Debería ser %s.",
                    ipPadreStr, prefijoPadre, calculatedNetworkIp);
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            etIp.setError("No es dir. de red para /" + prefijoPadre);
            etIp.requestFocus();
            return;
        } else {
            etIp.setError(null); // Limpiar error
        }


        // Crear IPAddress padre (ahora sabemos que los datos son válidos)
        redPadre = new IPAddress(ipPadreStr, prefijoPadre);


        // --- Recoger y validar datos de subredes dinámicas ---
        List<Subred> subredesInput = new ArrayList<>();
        int childCount = subnetContainer.getChildCount();
        if (childCount < 1) {
            Toast.makeText(this, "Añada al menos una subred", Toast.LENGTH_SHORT).show();
            // Podríamos enfocar el botón de añadir o el campo de número
            etNumSubnet.requestFocus();
            return;
        }

        long hostsTotalesSolicitados = 0; // Usar long para evitar overflow

        for (int i = 0; i < childCount; i++) {
            View child = subnetContainer.getChildAt(i);
            EditText etName = null;
            EditText etSizeInput = null;

            // Encontrar los EditText dentro del LinearLayout hijo
            if (child instanceof LinearLayout) {
                LinearLayout pairLayout = (LinearLayout) child;
                if (pairLayout.getChildCount() == 2) {
                    View child1 = pairLayout.getChildAt(0);
                    View child2 = pairLayout.getChildAt(1);
                    if (child1 instanceof EditText && child2 instanceof EditText) {
                        etName = (EditText) child1;
                        etSizeInput = (EditText) child2;
                    }
                }
            }

            // Si no se encontraron los EditText, hay un problema estructural
            if (etName == null || etSizeInput == null) {
                Toast.makeText(this, "Error interno leyendo estructura de subred " + (i + 1), Toast.LENGTH_SHORT).show();
                return; // Salir si hay error
            }

            // Validar nombre
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Nombre requerido para subred " + (i + 1), Toast.LENGTH_SHORT).show();
                etName.setError("Requerido");
                etName.requestFocus();
                return;
            } else {
                etName.setError(null);
            }

            // Validar tamaño
            String sizeStr = etSizeInput.getText().toString().trim();
            if (sizeStr.isEmpty()) {
                Toast.makeText(this, "Tamaño requerido para subred '" + name + "'", Toast.LENGTH_SHORT).show();
                etSizeInput.setError("Requerido");
                etSizeInput.requestFocus();
                return;
            }
            try {
                int size = Integer.parseInt(sizeStr);
                if (size <= 0) {
                    Toast.makeText(this, "Tamaño debe ser > 0 para subred '" + name + "'", Toast.LENGTH_SHORT).show();
                    etSizeInput.setError("Debe ser > 0");
                    etSizeInput.requestFocus();
                    return;
                }
                // Validar tamaño máximo razonable (ej: no más grande que una /8)
                if (size > (Math.pow(2, 24) - 2)) { // Más grande que una /8 es irreal
                    Toast.makeText(this, "Tamaño excesivo para subred '" + name + "'", Toast.LENGTH_SHORT).show();
                    etSizeInput.setError("Tamaño muy grande");
                    etSizeInput.requestFocus();
                    return;
                }

                subredesInput.add(new Subred(name, size));
                hostsTotalesSolicitados += size; // Sumar hosts solicitados (no tamaño de bloque aún)
                etSizeInput.setError(null); // Limpiar error
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Tamaño inválido para subred '" + name + "'", Toast.LENGTH_SHORT).show();
                etSizeInput.setError("Número inválido");
                etSizeInput.requestFocus();
                return;
            }
        }
        // --- FIN Recoger y validar datos de subredes ---

        hideKeyboard();
        // --- VALIDACIÓN DE ESPACIO TOTAL ---
        // Calcular espacio total disponible en la red padre (usar double para precisión)
        double totalAvailableAddresses = Math.pow(2, 32 - prefijoPadre);

        // Calcular espacio total requerido (sumando bloques potencia de 2)
        double totalRequiredBlockAddresses = 0;
        for (Subred subred : subredesInput) {
            int hostsSolicitados = subred.getSize();
            // bitsNecesarios calcula los bits de HOST k, tal que 2^k >= hosts+2
            int bitsParaHosts = Converters.bitsNecesarios(hostsSolicitados); // Usa tu clase Converters
            // El tamaño del bloque asignado (incluyendo red/broadcast) es 2^k
            double tamanoBloque = Math.pow(2, bitsParaHosts);
            totalRequiredBlockAddresses += tamanoBloque;
        }

        // Comparar espacio requerido (bloques) vs. disponible
        if (totalRequiredBlockAddresses > totalAvailableAddresses) {
            String errorMessage = String.format(Locale.getDefault(),
                    "Espacio insuficiente. Requerido (bloques): %.0f direcciones. Disponible: %.0f.",
                    totalRequiredBlockAddresses, totalAvailableAddresses);
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            // Podríamos poner error en IP/Prefijo o en el número de subredes
            etIp.setError("Espacio insuficiente para estas subredes");
            etIp.requestFocus();
            return; // Detener el proceso si no hay espacio suficiente
        }
        // --- FIN VALIDACIÓN DE ESPACIO TOTAL ---


        // --- 2. Ejecutar Lógica VLSM ---
        List<Subred> resultados;
        try {
            // Crear el objeto VLSM sólo si la validación de espacio pasó
            VLSM vlsmCalculator = new VLSM(redPadre); // Usa la IPAddress padre validada

            // Agregar las subredes validadas al calculador (ya están ordenadas por defecto si usas el método)
            for (Subred subred : subredesInput) {
                vlsmCalculator.agregarSubred(subred.getNombre(), subred.getSize());
            }

            // Obtener los resultados (la clase VLSM interna maneja la asignación)
            resultados = vlsmCalculator.getSubredes(); // Esto dispara el cálculo interno

            // Comprobar si *después* del cálculo, alguna subred no pudo ser asignada
            int asignadas = 0;
            boolean calculoExitosoCompleto = true;
            if (resultados != null) {
                for (Subred res : resultados) {
                    // Consideramos asignada si tiene una IP válida (no 0.0.0.0 y prefijo > 0)
                    if (res.getDireccion() != null && !res.getDireccion().getIp().equals("0.0.0.0") && res.getDireccion().getPrefijo() > 0) {
                        asignadas++;
                    } else {
                        calculoExitosoCompleto = false;
                    }
                }
                // Si el número de resultados no coincide con el input, algo falló también
                if (resultados.size() != subredesInput.size()){
                    calculoExitosoCompleto = false;
                }

            } else {
                // Si resultados es null, el cálculo falló completamente
                calculoExitosoCompleto = false;
                resultados = new ArrayList<>(); // Crear lista vacía para evitar NullPointerException abajo
            }


            // Mostrar advertencia si no todas fueron asignadas o si el cálculo falló
            if (!calculoExitosoCompleto && !subredesInput.isEmpty()) {
                String warningMsg;
                if (asignadas > 0) {
                    warningMsg = String.format(Locale.getDefault(),
                            "Aviso: No se pudo asignar IP a %d de %d subredes (posible fragmentación/error).",
                            subredesInput.size() - asignadas, subredesInput.size());
                } else {
                    warningMsg = "Error: No se pudo asignar ninguna subred. Verifique los tamaños o el espacio disponible.";
                }
                Toast.makeText(this, warningMsg, Toast.LENGTH_LONG).show();
            } else if (resultados.isEmpty() && !subredesInput.isEmpty()){
                Toast.makeText(this, "Error: El cálculo VLSM no produjo resultados.", Toast.LENGTH_LONG).show();
            }

            // --- 3. Mostrar Resultados ---
            displayResults(resultados); // Mostrar lo que se haya calculado (incluso si hay fallos)

        } catch (Exception e) {
            // Captura errores inesperados durante la creación de VLSM o el cálculo
            Toast.makeText(this, "Error inesperado durante cálculo VLSM: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace(); // Importante para depuración
            // Limpiar resultados por si acaso
            resultsDisplayContainer.removeAllViews();
            tvResultsHeader.setVisibility(View.GONE);
        }
    }

    /**
     * Muestra los resultados del cálculo con estilo mejorado y añade botón de reseteo.
     *
     * @param results La lista de subredes calculadas (puede contener fallos parciales).
     */
    private void displayResults(List<Subred> results) {
        resultsDisplayContainer.removeAllViews(); // Limpieza previa

        boolean hasResultsToShow = results != null && !results.isEmpty();

        if (!hasResultsToShow) {
            tvResultsHeader.setVisibility(View.GONE);
            // Opcional: Mostrar mensaje si no hay resultados pero se esperaban
            if (subnetContainer.getChildCount() > 0) { // Si el usuario pidió subredes
                TextView noResultsTextView = new TextView(this);
                LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                params.setMargins(0, dpToPx(16), 0, 0); // Margen superior
                noResultsTextView.setLayoutParams(params);
                noResultsTextView.setText(getString(R.string.no_results_to_display)); // Definir en strings.xml
                noResultsTextView.setGravity(Gravity.CENTER_HORIZONTAL); // Centrar texto
                noResultsTextView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
                resultsDisplayContainer.addView(noResultsTextView);
            }
            return; // No añadir botón de reseteo si no hay resultados
        }

        // Mostrar el título si hay resultados
        tvResultsHeader.setVisibility(View.VISIBLE);

        // Iterar y mostrar cada resultado
        for (int i = 0; i < results.size(); i++) {
            Subred subred = results.get(i);
            IPAddress direccion = subred.getDireccion(); // Puede ser la IP "0.0.0.0" si falló

            TextView resultTextView = new TextView(this);
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            // Añadir margen inferior entre resultados, pero no después del último
            int marginBottomPx = (i == results.size() - 1) ? 0 : dpToPx(12);
            params.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), marginBottomPx); // Márgenes laterales y superior/inferior
            resultTextView.setLayoutParams(params);
            resultTextView.setTextIsSelectable(true);
            // Añadir padding interno
            resultTextView.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
            // Establecer fondo y borde (requiere crear un drawable)
            resultTextView.setBackgroundResource(R.drawable.result_item_border); // Crear drawable res/drawable/result_item_border.xml

            StringBuilder sb = new StringBuilder();
            // Usar negrita para las etiquetas clave
            sb.append("<b>Subred:</b> ").append(sanitizeHtml(subred.getNombre())).append("<br/>"); // Sanitizar nombre
            sb.append("<b>Hosts Solicitados:</b> ").append(subred.getSize());

            boolean asignada = (direccion != null && !direccion.getIp().equals("0.0.0.0") && direccion.getPrefijo() > 0);

            if (asignada) {
                // Calcular hosts disponibles reales en la subred asignada
                // Usar long para el cálculo de potencia para evitar overflow con prefijos pequeños
                long hostsDisponiblesCalc = (long) Math.pow(2, 32 - direccion.getPrefijo()) - 2;
                int hostsDisponibles = (hostsDisponiblesCalc < 0) ? 0 : (int) hostsDisponiblesCalc; // Convertir a int si es positivo

                sb.append(" (<b>Disponibles:</b> ").append(hostsDisponibles).append(")<br/>"); // Añadir hosts reales
                sb.append("  <b>Red Asignada:</b> ").append(direccion.getIp()).append("/").append(direccion.getPrefijo()).append("<br/>");
                sb.append("  <b>Máscara:</b> ").append(direccion.getMascara()).append("<br/>");

                // Mostrar rango solo si hay hosts disponibles
                if (hostsDisponibles > 0 && direccion.getPrefijo() <= 30) { // Rango usable solo hasta /30
                    sb.append("  <b>Rango Hosts:</b> ").append(direccion.getFirstDir()).append(" - ").append(direccion.getLastDir()).append("<br/>");
                } else {
                    sb.append("  <b>Rango Hosts:</b> N/A<br/>");
                }
                sb.append("  <b>Broadcast:</b> ").append(direccion.getBroadcast());

                // Color de texto normal
                resultTextView.setTextColor(ContextCompat.getColor(this, R.color.result_text_color)); // Definir en colors.xml

            } else {
                sb.append("<br/>"); // Salto de línea antes del mensaje de error
                sb.append("  <font color='#D32F2F'><b>Red Asignada: ¡No se pudo asignar!</b></font>"); // Color rojo oscuro
                // Cambiar fondo o texto para destacar el error
                resultTextView.setTextColor(ContextCompat.getColor(this, R.color.result_error_text_color)); // Definir color diferente si se quiere
                // Podríamos cambiar el fondo también: resultTextView.setBackgroundResource(R.drawable.result_item_error_border);
            }
            // Usar Html.fromHtml para interpretar las etiquetas
            // FROM_HTML_MODE_COMPACT es bueno para TextViews
            resultTextView.setText(Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_COMPACT));

            resultsDisplayContainer.addView(resultTextView);
        }

        // --- Añadir Botón "Nuevo Cálculo" ---
        if (btnNewCalculation == null) { // Crear el botón solo una vez
            btnNewCalculation = new Button(this);
            LayoutParams btnParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            btnParams.gravity = Gravity.CENTER_HORIZONTAL; // Centrar el botón
            btnParams.setMargins(0, dpToPx(24), 0, dpToPx(16)); // Márgenes superior/inferior
            btnNewCalculation.setLayoutParams(btnParams);
            btnNewCalculation.setText(getString(R.string.new_calculation_button)); // Definir en strings.xml
            // Aplicar estilo (opcional, podrías usar un estilo definido en styles.xml)
            // btnNewCalculation.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.button_background_color));
            // btnNewCalculation.setTextColor(ContextCompat.getColor(this, R.color.button_text_color));
            btnNewCalculation.setOnClickListener(v -> resetCalculatorState(true)); // Llamar a reset con Toast
        }

        // Asegurarse que el botón no esté ya añadido (por si acaso)
        if (btnNewCalculation.getParent() == null) {
            resultsDisplayContainer.addView(btnNewCalculation);
        }
        btnNewCalculation.setVisibility(View.VISIBLE); // Asegurarse que sea visible

    }

    /**
     * Convierte unidades dp a píxeles de forma segura.
     */
    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        // Usar Math.round para evitar posibles errores de casteo y asegurar > 0
        return Math.max(1, Math.round(dp * displayMetrics.density));
    }

    /**
     * Valida si una cadena tiene el formato de una dirección IPv4.
     * (No valida si es pública/privada, solo formato).
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        try {
            // InetAddress.getByName puede resolver nombres de host, así que verificamos
            // si la cadena original coincide con la IP resuelta.
            // Usamos una expresión regular simple para una validación de formato más estricta primero.
            String zeroTo255 = "(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])";
            String ipPattern = zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255;
            if (!ip.matches(ipPattern)) {
                return false;
            }
            // Si pasa el regex, intentamos parsear para confirmar
            // InetAddress obj = InetAddress.getByName(ip);
            // return obj.getHostAddress().equals(ip); // Comprueba que no se resolvió a otra cosa
            return true; // El regex es suficiente para el formato
        } catch (Exception e) {
            return false; // Si regex falla o hay otro error
        }
    }

    /**
     * Calcula la dirección de red para una IP y prefijo dados.
     */
    private String calculateNetworkAddress(String ipStr, int prefix) {
        try {
            InetAddress ip = InetAddress.getByName(ipStr);
            byte[] ipBytes = ip.getAddress();

            // Asegurarse que sea IPv4
            if (ipBytes.length != 4) {
                return ipStr; // Devolver original si no es IPv4? O lanzar error?
            }

            // Crear máscara de red en formato entero
            // 0xFFFFFFFF es -1 en complemento a dos
            int mask = (prefix == 0) ? 0 : (0xFFFFFFFF << (32 - prefix));

            // Aplicar máscara (operación AND a nivel de bits)
            ByteBuffer buffer = ByteBuffer.wrap(ipBytes);
            int ipInt = buffer.getInt();
            int networkInt = ipInt & mask;

            // Convertir entero de red de nuevo a bytes y luego a InetAddress
            byte[] networkBytes = ByteBuffer.allocate(4).putInt(networkInt).array();
            return InetAddress.getByAddress(networkBytes).getHostAddress();

        } catch (UnknownHostException e) {
            // Esto no debería pasar si la IP ya fue validada antes con regex
            return "0.0.0.0"; // O un valor que indique error
        } catch (Exception e) {
            // Capturar otros posibles errores
            e.printStackTrace();
            return "0.0.0.0";
        }
    }

    /**
     * Simple sanitization for HTML display. Replace '<' and '>' to prevent issues.
     */
    private String sanitizeHtml(String input) {
        if (input == null) return "";
        return input.replace("<", "&lt;").replace(">", "&gt;");
    }

} // Fin de la clase MainActivity