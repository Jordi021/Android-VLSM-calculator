package com.androidvlsm;

import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

// Imports de AndroidX
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // Para colores

// Imports de utils
import com.androidvlsm.utils.IPAddress;
import com.androidvlsm.utils.Subred;
import com.androidvlsm.utils.VLSM;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    // Constantes
    private final int DEFAULT_SUBNETS = 4; // Usado como fallback o para lógica interna
    private final int MIN_SUBNETS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Referencias a Vistas ---
        subnetContainer = findViewById(R.id.subnetContainer);
        etIp = findViewById(R.id.etIp);
        etPrefix = findViewById(R.id.etPrefix);
        etNumSubnet = findViewById(R.id.numSubnet);
        btnChangeSubnets = findViewById(R.id.btnChang);
        btnCalculate = findViewById(R.id.btnCalc);
        resultsDisplayContainer = findViewById(R.id.resultsDisplayContainer);
        tvResultsHeader = findViewById(R.id.textViewResultsHeader);

        // --- Estado Inicial ---
        if (savedInstanceState == null) {
            int initialSubnets;
            try {
                initialSubnets = Integer.parseInt(etNumSubnet.getText().toString());
                if (initialSubnets < MIN_SUBNETS) initialSubnets = MIN_SUBNETS;
            } catch (NumberFormatException e) {
                initialSubnets = DEFAULT_SUBNETS;
                etNumSubnet.setText(String.valueOf(initialSubnets));
            }
            updateSubnetViews(initialSubnets);
        }

        // --- Listeners ---
        btnChangeSubnets.setOnClickListener(v -> handleChangeButtonClick());
        btnCalculate.setOnClickListener(v -> handleCalculateButtonClick());
    }

    /**
     * Botón "Cambiar".
     */
    private void handleChangeButtonClick() {
        int targetCount;
        String numSubnetStr = etNumSubnet.getText().toString().trim();

        if (numSubnetStr.isEmpty()) {
            numSubnetStr = String.valueOf(DEFAULT_SUBNETS);
            etNumSubnet.setText(numSubnetStr);
        }

        try {
            targetCount = Integer.parseInt(numSubnetStr);
            if (targetCount < MIN_SUBNETS) {
                targetCount = MIN_SUBNETS;
                etNumSubnet.setText(String.valueOf(targetCount)); // Corregir en UI
                Toast.makeText(this, "El número mínimo de subredes es " + MIN_SUBNETS, Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Número inválido", Toast.LENGTH_SHORT).show();
            try {
                targetCount = subnetContainer.getChildCount();
            } catch (Exception ignored) {
                targetCount = DEFAULT_SUBNETS;
            }
            etNumSubnet.setText(String.valueOf(targetCount));
            etNumSubnet.setError("Número inválido");
            return;
        }
        etNumSubnet.setError(null);
        updateSubnetViews(targetCount);
    }

    /**
     * Sincroniza la UI para que muestre 'targetCount' pares de EditText.
     */
    private void updateSubnetViews(int targetCount) {
        int currentCount = subnetContainer.getChildCount();
        if (targetCount > currentCount) {
            for (int i = currentCount; i < targetCount; i++) {
                addSubnetPair(i);
            }
        } else if (targetCount < currentCount) {
            for (int i = currentCount - 1; i >= targetCount; i--) {
                if (subnetContainer.getChildAt(i) != null) {
                    subnetContainer.removeViewAt(i);
                }
            }
        }

    }

    /**
     * Crea y añade un par de EditText (Nombre, Tamaño) al contenedor.
     */
    private void addSubnetPair(int index) {
        LinearLayout pairLayout = new LinearLayout(this);
        pairLayout.setOrientation(LinearLayout.HORIZONTAL);
        LayoutParams pairParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        if (index > 0) {
            pairParams.setMargins(0, dpToPx(8), 0, 0);
        }
        pairLayout.setLayoutParams(pairParams);

        // EditText para Nombre (Net)
        EditText etNetName = new EditText(this);
        LayoutParams etNameParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f); // Usar weight para distribuir espacio
        etNameParams.setMarginEnd(dpToPx(8)); // Margen entre nombre y tamaño
        etNetName.setLayoutParams(etNameParams);
        char labelChar = (char) ('A' + index);
        // Usar recursos de String es preferible
        etNetName.setHint(getString(R.string.hint_subnet_name));
        etNetName.setText(getString(R.string.subnet_label_prefix, labelChar));
        etNetName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etNetName.setId(View.generateViewId()); // Generar ID único

        // EditText para Tamaño (Size)
        EditText etNetSize = new EditText(this);
        LayoutParams etSizeParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f); // Usar weight
        etNetSize.setLayoutParams(etSizeParams);
        etNetSize.setText(""); // Empezar vacío
        etNetSize.setHint(getString(R.string.hint_subnet_size)); // Asegúrate que exista R.string.hint_subnet_size
        etNetSize.setInputType(InputType.TYPE_CLASS_NUMBER);
        etNetSize.setId(View.generateViewId()); // Generar ID único

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
        tvResultsHeader.setVisibility(View.GONE); // Ocultar título

        // --- 1. Recoger y Validar Datos de la Interfaz ---
        String ipPadreStr = etIp.getText().toString().trim();
        String prefijoPadreStr = etPrefix.getText().toString().trim();
        int prefijoPadre;
        IPAddress redPadre;

        // Validaciones IP/Prefijo Padre
        if (ipPadreStr.isEmpty()) {
            Toast.makeText(this, "IP padre requerida", Toast.LENGTH_SHORT).show();
            etIp.setError("Requerido");
            etIp.requestFocus();
            return;
        } else {
            etIp.setError(null);
        }
        if (prefijoPadreStr.isEmpty()) {
            Toast.makeText(this, "Prefijo padre requerido", Toast.LENGTH_SHORT).show();
            etPrefix.setError("Requerido");
            etPrefix.requestFocus();
            return;
        } else {
            etPrefix.setError(null);
        }

        try {
            prefijoPadre = Integer.parseInt(prefijoPadreStr);
            if (ipPadreStr.split("\\.").length != 4)
                throw new IllegalArgumentException("Formato IP inválido");
            if (prefijoPadre < 1 || prefijoPadre > 30)
                throw new IllegalArgumentException("Prefijo padre inválido (1-30)");
            redPadre = new IPAddress(ipPadreStr, prefijoPadre);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Prefijo padre debe ser número", Toast.LENGTH_SHORT).show();
            etPrefix.setError("Número inválido");
            etPrefix.requestFocus();
            return;
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            if (e.getMessage().contains("IP")) {
                etIp.setError(e.getMessage());
                etIp.requestFocus();
            } else {
                etPrefix.setError(e.getMessage());
                etPrefix.requestFocus();
            }
            return;
        }

        // Recoger y validar datos de subredes dinámicas
        List<Subred> subredesInput = new ArrayList<>();
        int childCount = subnetContainer.getChildCount();
        if (childCount < 1) {
            Toast.makeText(this, "Añada al menos una subred", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < childCount; i++) {
            View child = subnetContainer.getChildAt(i);
            EditText etName = null;
            EditText etSizeInput = null;

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

            if (etName == null || etSizeInput == null) {
                Toast.makeText(this, "Error interno leyendo subred " + (i + 1), Toast.LENGTH_SHORT).show();
                return;
            }

            String name = etName.getText().toString().trim();
            String sizeStr = etSizeInput.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Nombre vacío (subred " + (i + 1) + ")", Toast.LENGTH_SHORT).show();
                etName.setError("Requerido");
                etName.requestFocus();
                return;
            } else {
                etName.setError(null);
            }
            if (sizeStr.isEmpty()) {
                Toast.makeText(this, "Tamaño vacío (subred " + name + ")", Toast.LENGTH_SHORT).show();
                etSizeInput.setError("Requerido");
                etSizeInput.requestFocus();
                return;
            } else {
                etSizeInput.setError(null);
            }

            try {
                int size = Integer.parseInt(sizeStr);
                if (size <= 0) {
                    Toast.makeText(this, "Tamaño debe ser > 0 (" + name + ")", Toast.LENGTH_SHORT).show();
                    etSizeInput.setError("Debe ser > 0");
                    etSizeInput.requestFocus();
                    return;
                }
                subredesInput.add(new Subred(name, size));
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Tamaño inválido (" + name + ")", Toast.LENGTH_SHORT).show();
                etSizeInput.setError("Número inválido");
                etSizeInput.requestFocus();
                return;
            }
        }

        // --- 2. Ejecutar Lógica VLSM ---
        List<Subred> resultados;
        try {
            VLSM vlsmCalculator = new VLSM(redPadre);

            for (Subred subred : subredesInput) {
                vlsmCalculator.agregarSubred(subred.getNombre(), subred.getSize());
            }

            resultados = vlsmCalculator.getSubredes();

            boolean calculoExitoso = true;
            if (resultados.isEmpty() && !subredesInput.isEmpty()) {
                calculoExitoso = false;
            } else {
                for (Subred res : resultados) {
                    if (res.getDireccion() == null || res.getDireccion().getIp().equals("0.0.0.0")) {
                        calculoExitoso = false;
                        break;
                    }
                }
            }

            if (!calculoExitoso) {
                Toast.makeText(this, "Aviso: No se pudo asignar IP a todas las subredes solicitadas (espacio insuficiente?).", Toast.LENGTH_LONG).show();
            }

            // --- 3. Mostrar Resultados ---
            displayResults(resultados); // Mostrar lo que se haya calculado

        } catch (Exception e) {
            Toast.makeText(this, "Error inesperado en cálculo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Muestra los resultados del cálculo.
     *
     * @param results La lista de subredes calculadas (puede contener fallos parciales).
     */
    private void displayResults(List<Subred> results) {
        resultsDisplayContainer.removeAllViews(); // limpieza previa

        if (results == null || results.isEmpty()) {
            tvResultsHeader.setVisibility(View.GONE);
            return;
        }

        tvResultsHeader.setVisibility(View.VISIBLE);

        for (int i = 0; i < results.size(); i++) {
            Subred subred = results.get(i);
            IPAddress direccion = subred.getDireccion();

            TextView resultTextView = new TextView(this);
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, dpToPx(16)); // Margen inferior
            resultTextView.setLayoutParams(params);
            resultTextView.setTextIsSelectable(true);

            StringBuilder sb = new StringBuilder();
            sb.append("Subred: ").append(subred.getNombre()).append("\n");
            sb.append("Hosts Solicitados: ").append(subred.getSize()); // No añadir \n aún


            if (direccion != null && !direccion.getIp().equals("0.0.0.0")) {
                sb.append("\n") // Añadir salto de línea ahora
                        .append("  Red Asignada: ").append(direccion.getIp()).append("/").append(direccion.getPrefijo()).append("\n")
                        .append("  Máscara: ").append(direccion.getMascara()).append("\n")
                        .append("  Rango Hosts: ").append(direccion.getFirstDir()).append(" - ").append(direccion.getLastDir()).append("\n")
                        .append("  Broadcast: ").append(direccion.getBroadcast());

            } else {
                sb.append("\n")
                        .append("  Red Asignada: ¡No se pudo asignar!");
                resultTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            }
            resultTextView.setText(sb.toString());
            resultsDisplayContainer.addView(resultTextView);
        }
    }

    /**
     * Convierte unidades dp a píxeles.
     */
    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * displayMetrics.density);
    }
}