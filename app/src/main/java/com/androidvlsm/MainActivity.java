package com.androidvlsm;

import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity;
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
import androidx.core.content.ContextCompat;

// Imports de utils
import com.androidvlsm.utils.Converters;
import com.androidvlsm.utils.IPAddress;
import com.androidvlsm.utils.Subred;
import com.androidvlsm.utils.VLSM;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {


    private LinearLayout subnetContainer;
    private EditText etIp;
    private EditText etPrefix;
    private EditText etNumSubnet;
    private Button btnChangeSubnets;
    private Button btnCalculate;
    private LinearLayout resultsDisplayContainer;
    private TextView tvResultsHeader;
    private Button btnNewCalculation;

    private final int DEFAULT_SUBNETS = 4;
    private final int MIN_SUBNETS = 1;
    private final int MAX_SUBNETS = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        subnetContainer = findViewById(R.id.subnetContainer);
        etIp = findViewById(R.id.etIp);
        etPrefix = findViewById(R.id.etPrefix);
        etNumSubnet = findViewById(R.id.numSubnet);
        btnChangeSubnets = findViewById(R.id.btnChang);
        btnCalculate = findViewById(R.id.btnCalc);
        resultsDisplayContainer = findViewById(R.id.resultsDisplayContainer);
        tvResultsHeader = findViewById(R.id.textViewResultsHeader);

        if (savedInstanceState == null) {
            resetCalculatorState(false); // Usa el método de reseteo para el estado inicial
        }

        btnChangeSubnets.setOnClickListener(v -> handleChangeButtonClick());
        btnCalculate.setOnClickListener(v -> handleCalculateButtonClick());

        tvResultsHeader.setVisibility(View.GONE);
    }

    /**
     * Resetea la interfaz a su estado inicial, limpia todos los campos
     * y oculta el teclado.
     * @param showToast Indica si mostrar un Toast de confirmación.
     */
    private void resetCalculatorState(boolean showToast) {
        etIp.setText("");
        etPrefix.setText("");
        etIp.setError(null);
        etPrefix.setError(null);
        etNumSubnet.setError(null);

        resultsDisplayContainer.removeAllViews();
        tvResultsHeader.setVisibility(View.GONE);
        if (btnNewCalculation != null) {
            btnNewCalculation.setVisibility(View.GONE);
        }

        etNumSubnet.setText(String.valueOf(DEFAULT_SUBNETS));

        subnetContainer.removeAllViews();

        updateSubnetViews(DEFAULT_SUBNETS);

        hideKeyboard();

        etIp.requestFocus();

        if (showToast) {
            Toast.makeText(this, "Formulario reiniciado", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Oculta el teclado virtual si está visible.
     */
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            view.clearFocus();
        } else {
            view = findViewById(android.R.id.content);
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
            etNumSubnet.setText(numSubnetStr);
        }

        try {
            targetCount = Integer.parseInt(numSubnetStr);
            if (targetCount < MIN_SUBNETS) {
                targetCount = MIN_SUBNETS;
                etNumSubnet.setText(String.valueOf(targetCount));
                etNumSubnet.setError("Mínimo " + MIN_SUBNETS);
                Toast.makeText(this, "El número mínimo de subredes es " + MIN_SUBNETS, Toast.LENGTH_SHORT).show();
                return;
            } else if (targetCount > MAX_SUBNETS) {
                targetCount = MAX_SUBNETS;
                etNumSubnet.setText(String.valueOf(targetCount));
                etNumSubnet.setError("Máximo " + MAX_SUBNETS);
                Toast.makeText(this, "El número máximo de subredes es " + MAX_SUBNETS, Toast.LENGTH_SHORT).show();
                return;
            } else {
                etNumSubnet.setError(null);
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Número de subredes inválido", Toast.LENGTH_SHORT).show();
            etNumSubnet.setError("Número inválido");
            etNumSubnet.requestFocus();
            return;
        }

        updateSubnetViews(targetCount);
    }

    /**
     * Sincroniza la UI para que muestre 'targetCount' pares de EditText.
     * Limpia los campos al añadir/quitar.
     */
    private void updateSubnetViews(int targetCount) {
        int currentCount = subnetContainer.getChildCount();

        if (targetCount > currentCount) {
            for (int i = currentCount; i < targetCount; i++) {
                addSubnetPair(i);
            }
        }
        else if (targetCount < currentCount) {
            for (int i = currentCount - 1; i >= targetCount; i--) {
                // Es más seguro quitar desde el final
                if (subnetContainer.getChildAt(i) != null) {
                    subnetContainer.removeViewAt(i);
                }
            }
        }

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
        if (index > 0) {
            pairParams.setMargins(0, dpToPx(8), 0, 0);
        }
        pairLayout.setLayoutParams(pairParams);

        EditText etNetName = new EditText(this);
        LayoutParams etNameParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 0.5f);
        etNameParams.setMarginEnd(dpToPx(8));
        etNetName.setLayoutParams(etNameParams);
        char labelChar = (char) ('A' + index);
        etNetName.setHint(getString(R.string.hint_subnet_name));
        etNetName.setText(getString(R.string.subnet_label_prefix, labelChar));
        etNetName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etNetName.setId(View.generateViewId());

        EditText etNetSize = new EditText(this);
        LayoutParams etSizeParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 0.5f);
        etNetSize.setLayoutParams(etSizeParams);
        etNetSize.setText("");
        etNetSize.setHint(getString(R.string.hint_subnet_size));
        etNetSize.setInputType(InputType.TYPE_CLASS_NUMBER);
        etNetSize.setId(View.generateViewId());

        pairLayout.addView(etNetName);
        pairLayout.addView(etNetSize);
        subnetContainer.addView(pairLayout);
    }

    /**
     * Botón "Calcular".
     */
    private void handleCalculateButtonClick() {
        resultsDisplayContainer.removeAllViews();
        tvResultsHeader.setVisibility(View.GONE);

        String ipPadreStr = etIp.getText().toString().trim();
        String prefijoPadreStr = etPrefix.getText().toString().trim();
        int prefijoPadre;
        IPAddress redPadre;


        if (!isValidIpAddress(ipPadreStr)) {
            Toast.makeText(this, "Formato de IP padre inválido", Toast.LENGTH_SHORT).show();
            etIp.setError("Formato inválido (ej: 192.168.1.0)");
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
        }
        try {
            prefijoPadre = Integer.parseInt(prefijoPadreStr);
            if (prefijoPadre < 1 || prefijoPadre > 30) {
                throw new IllegalArgumentException("Prefijo padre debe estar entre 1 y 30");
            }
            etPrefix.setError(null);
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
            etIp.setError(null);
        }



        redPadre = new IPAddress(ipPadreStr, prefijoPadre);


        List<Subred> subredesInput = new ArrayList<>();
        int childCount = subnetContainer.getChildCount();
        if (childCount < 1) {
            Toast.makeText(this, "Añada al menos una subred", Toast.LENGTH_SHORT).show();

            etNumSubnet.requestFocus();
            return;
        }

        long hostsTotalesSolicitados = 0;

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
                Toast.makeText(this, "Error interno leyendo estructura de subred " + (i + 1), Toast.LENGTH_SHORT).show();
                return; // Salir si hay error
            }


            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Nombre requerido para subred " + (i + 1), Toast.LENGTH_SHORT).show();
                etName.setError("Requerido");
                etName.requestFocus();
                return;
            } else {
                etName.setError(null);
            }


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

                if (size > (Math.pow(2, 24) - 2)) {
                    Toast.makeText(this, "Tamaño excesivo para subred '" + name + "'", Toast.LENGTH_SHORT).show();
                    etSizeInput.setError("Tamaño muy grande");
                    etSizeInput.requestFocus();
                    return;
                }

                subredesInput.add(new Subred(name, size));
                hostsTotalesSolicitados += size;
                etSizeInput.setError(null);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Tamaño inválido para subred '" + name + "'", Toast.LENGTH_SHORT).show();
                etSizeInput.setError("Número inválido");
                etSizeInput.requestFocus();
                return;
            }
        }


        hideKeyboard();
        double totalAvailableAddresses = Math.pow(2, 32 - prefijoPadre);

        double totalRequiredBlockAddresses = 0;
        for (Subred subred : subredesInput) {
            int hostsSolicitados = subred.getSize();

            int bitsParaHosts = Converters.bitsNecesarios(hostsSolicitados);
            double tamanoBloque = Math.pow(2, bitsParaHosts);
            totalRequiredBlockAddresses += tamanoBloque;
        }


        if (totalRequiredBlockAddresses > totalAvailableAddresses) {
            String errorMessage = String.format(Locale.getDefault(),
                    "Espacio insuficiente. Requerido (bloques): %.0f direcciones. Disponible: %.0f.",
                    totalRequiredBlockAddresses, totalAvailableAddresses);
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();

            etIp.setError("Espacio insuficiente para estas subredes");
            etIp.requestFocus();
            return;
        }

        List<Subred> resultados;
        try {

            VLSM vlsmCalculator = new VLSM(redPadre);


            for (Subred subred : subredesInput) {
                vlsmCalculator.agregarSubred(subred.getNombre(), subred.getSize());
            }

            resultados = vlsmCalculator.getSubredes();

            int asignadas = 0;
            boolean calculoExitosoCompleto = true;
            if (resultados != null) {
                for (Subred res : resultados) {
                    if (res.getDireccion() != null && !res.getDireccion().getIp().equals("0.0.0.0") && res.getDireccion().getPrefijo() > 0) {
                        asignadas++;
                    } else {
                        calculoExitosoCompleto = false;
                    }
                }
                if (resultados.size() != subredesInput.size()){
                    calculoExitosoCompleto = false;
                }

            } else {
                calculoExitosoCompleto = false;
                resultados = new ArrayList<>();
            }


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

            displayResults(resultados);

        } catch (Exception e) {
            Toast.makeText(this, "Error inesperado durante cálculo VLSM: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            resultsDisplayContainer.removeAllViews();
            tvResultsHeader.setVisibility(View.GONE);
        }
    }

    /**
     * Muestra los resultados del cálculo y  botón de reseteo.
     *
     * @param results La lista de subredes calculadas (puede contener fallos parciales).
     */
    private void displayResults(List<Subred> results) {
        resultsDisplayContainer.removeAllViews();

        boolean hasResultsToShow = results != null && !results.isEmpty();

        if (!hasResultsToShow) {
            tvResultsHeader.setVisibility(View.GONE);
            if (subnetContainer.getChildCount() > 0) {
                TextView noResultsTextView = new TextView(this);
                LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                params.setMargins(0, dpToPx(16), 0, 0);
                noResultsTextView.setLayoutParams(params);
                noResultsTextView.setText(getString(R.string.no_results_to_display));
                noResultsTextView.setGravity(Gravity.CENTER_HORIZONTAL);
                noResultsTextView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
                resultsDisplayContainer.addView(noResultsTextView);
            }
            return;
        }

        tvResultsHeader.setVisibility(View.VISIBLE);

        for (int i = 0; i < results.size(); i++) {
            Subred subred = results.get(i);
            IPAddress direccion = subred.getDireccion();

            TextView resultTextView = new TextView(this);
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            int marginBottomPx = (i == results.size() - 1) ? 0 : dpToPx(12);
            params.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), marginBottomPx);
            resultTextView.setLayoutParams(params);
            resultTextView.setTextIsSelectable(true);
            resultTextView.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
            resultTextView.setBackgroundResource(R.drawable.result_item_border);

            StringBuilder sb = new StringBuilder();
            sb.append("<b>Subred:</b> ").append(sanitizeHtml(subred.getNombre())).append("<br/>");
            sb.append("<b>Hosts Solicitados:</b> ").append(subred.getSize());

            boolean asignada = (direccion != null && !direccion.getIp().equals("0.0.0.0") && direccion.getPrefijo() > 0);

            if (asignada) {
                long hostsDisponiblesCalc = (long) Math.pow(2, 32 - direccion.getPrefijo()) - 2;
                int hostsDisponibles = (hostsDisponiblesCalc < 0) ? 0 : (int) hostsDisponiblesCalc;

                sb.append(" (<b>Disponibles:</b> ").append(hostsDisponibles).append(")<br/>");
                sb.append("  <b>Red Asignada:</b> ").append(direccion.getIp()).append("/").append(direccion.getPrefijo()).append("<br/>");
                sb.append("  <b>Máscara:</b> ").append(direccion.getMascara()).append("<br/>");


                if (hostsDisponibles > 0 && direccion.getPrefijo() <= 30) {
                    sb.append("  <b>Rango Hosts:</b> ").append(direccion.getFirstDir()).append(" - ").append(direccion.getLastDir()).append("<br/>");
                } else {
                    sb.append("  <b>Rango Hosts:</b> N/A<br/>");
                }
                sb.append("  <b>Broadcast:</b> ").append(direccion.getBroadcast());


                resultTextView.setTextColor(ContextCompat.getColor(this, R.color.result_text_color)); // Definir en colors.xml

            } else {
                sb.append("<br/>");
                sb.append("  <font color='#D32F2F'><b>Red Asignada: ¡No se pudo asignar!</b></font>");
                resultTextView.setTextColor(ContextCompat.getColor(this, R.color.result_error_text_color));
            }

            resultTextView.setText(Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_COMPACT));

            resultsDisplayContainer.addView(resultTextView);
        }

        if (btnNewCalculation == null) {
            btnNewCalculation = new Button(this);
            LayoutParams btnParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            btnParams.gravity = Gravity.CENTER_HORIZONTAL;
            btnParams.setMargins(0, dpToPx(24), 0, dpToPx(16));
            btnNewCalculation.setLayoutParams(btnParams);
            btnNewCalculation.setText(getString(R.string.new_calculation_button));
            btnNewCalculation.setOnClickListener(v -> resetCalculatorState(true));
        }

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
            String zeroTo255 = "(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])";
            String ipPattern = zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255;
            if (!ip.matches(ipPattern)) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calcula la dirección de red para una IP y prefijo dados.
     */
    private String calculateNetworkAddress(String ipStr, int prefix) {
        try {
            InetAddress ip = InetAddress.getByName(ipStr);
            byte[] ipBytes = ip.getAddress();

            if (ipBytes.length != 4) {
                return ipStr;
            }


            int mask = (prefix == 0) ? 0 : (0xFFFFFFFF << (32 - prefix));

            ByteBuffer buffer = ByteBuffer.wrap(ipBytes);
            int ipInt = buffer.getInt();
            int networkInt = ipInt & mask;

            byte[] networkBytes = ByteBuffer.allocate(4).putInt(networkInt).array();
            return InetAddress.getByAddress(networkBytes).getHostAddress();

        } catch (UnknownHostException e) {
            return "0.0.0.0";
        } catch (Exception e) {
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

}