package com.adrianorc.dolaragora;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends ActionBarActivity {

    private CharSequence mTitle;

    private EditText mEditTextBRL;
    private EditText mEditTextUSD;

    private TextView mTxtQuote;
    private TextView mTxtLastCheck;

    private CurrencyInfo mCurrencyInfo;
    private int mCurrentInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTitle = getTitle();
        mTxtQuote = (TextView) findViewById(R.id.txtQuote);
        mTxtLastCheck = (TextView) findViewById(R.id.txtLastCheck);

        mEditTextBRL = (EditText) findViewById(R.id.editBRL);
        mEditTextUSD = (EditText) findViewById(R.id.editUSD);

        mEditTextBRL.setHintTextColor(getResources().getColor(R.color.white));
        mEditTextUSD.setHintTextColor(getResources().getColor(R.color.white));

        mEditTextBRL.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mCurrentInput == 1) {
                    if (s.length() == 0) {
                        mEditTextUSD.setText("");
                    } else {
                        mEditTextUSD.setText(String.format("%1$.2f", Double.parseDouble(mEditTextBRL.getText().toString()) / mCurrencyInfo.USD));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mEditTextBRL.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mCurrentInput = 1;
                }
            }
        });

        mEditTextUSD.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mCurrentInput == 2) {
                    if (s.length() == 0) {
                        mEditTextBRL.setText("");
                    } else {
                        mEditTextBRL.setText(String.format("%1$.2f", Double.parseDouble(mEditTextUSD.getText().toString()) * mCurrencyInfo.USD));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mEditTextUSD.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mCurrentInput = 2;
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.global, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_update:
                refreshData();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    private void refreshData() {
        Toast.makeText(this, R.string.updating, Toast.LENGTH_SHORT).show();
        new RefreshTask().execute();
    }

    private void updateView() {
        String quote = String.format(getResources().getString(R.string.quoteMessage), mCurrencyInfo.USD);
        mTxtQuote.setText(quote);

        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String fmtDate = fmt.format(mCurrencyInfo.lastCheck);

        mTxtLastCheck.setText(getResources().getString(R.string.lastUpdateMessage).concat(fmtDate));
    }

    private void onUpdateQuote(boolean found) {
        if (found) {
            Toast.makeText(this, R.string.successful_update, Toast.LENGTH_SHORT).show();
            updateView();
        } else {
            Toast.makeText(this, R.string.failed_update, Toast.LENGTH_SHORT).show();
        }
    }

    public class RefreshTask extends AsyncTask<Void, Void, Void> {

        private static final String YAHOO_FINANCES_API = "http://finance.yahoo.com/webservice/v1/symbols/allcurrencies/quote";
        private static final String USD_BRL = "USD/BRL";
        private static final String NAME = "name";
        private static final String PRICE = "price";

        private boolean mFound = false;

        @Override
        protected Void doInBackground(Void... params) {

            boolean found = false;
            double price = 0d;

            try {
                URL url = new URL(YAHOO_FINANCES_API);
                URLConnection conn = url.openConnection();

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(conn.getInputStream());

                NodeList resources = doc.getElementsByTagName("resource");

                for (int i = 0; i < resources.getLength() && !found; i++) {
                    Element resource = (Element) resources.item(i);
                    NodeList fields = resource.getElementsByTagName("field");

                    for (int j = 0; j < fields.getLength() && !found; j++) {
                        Element field = (Element) fields.item(j);
                        String name = field.getAttribute("name");

                        if (name.equals(NAME)) {
                            if (field.getTextContent().toUpperCase().equals(USD_BRL)) {
                                Log.w("dolarHoje", "Quote USD/BRL found!");
                                found = true;
                                price = extractPriceFromResource(resource);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                found = false;
            }

            if (found) {
                mCurrencyInfo = new CurrencyInfo();
                mCurrencyInfo.USD = price;
                mCurrencyInfo.lastCheck = new Date();
                mFound = found;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            MainActivity.this.onUpdateQuote(mFound);
        }

        private double extractPriceFromResource(Element resource) throws Exception {
            NodeList fields = resource.getElementsByTagName("field");

            for (int j = 0; j < fields.getLength(); j++) {
                Element field = (Element) fields.item(j);
                if (field.getAttribute("name").equals(PRICE)) {
                    Log.w("dolarHoje", "Price for quote USD/BRL found = " + field.getTextContent());
                    String text = field.getTextContent();
                    if (text.contains(",")) {
                        text.replace(",", ".");
                    }
                    return Double.parseDouble(text);
                }
            }

            throw new Exception("Could not find price in API result");
        }

    }

}
