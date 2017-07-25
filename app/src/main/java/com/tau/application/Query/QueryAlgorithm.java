package com.tau.application.Query;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tau.application.DoctorMain;
import com.tau.application.R;
import com.tau.application.Smarteyeglass.SmarteyeglassUtils;
import com.tau.application.Utils.ManagePatients;
import com.tau.application.Utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.tau.application.Utils.Utils.log;

/**
 * Created by dan on 06/05/2017.
**/

public class QueryAlgorithm extends Activity implements QueryInterface {

    public int LIMIT = 40;

    private final String ENDPOINT_DGIDB = "DGIDB";
    private final String ENDPOINT_HGNC = "HGNC";
    private final String ENDPOINT_DISGENET1 = "DISGENET1";
    private final String ENDPOINT_DISGENET2 = "DISGENET2";

    private final String URL_DGIDB = "http://dgidb.genome.wustl.edu/api/v1/interactions.json?genes=%s";
    private final String URL_HGNC = "http://rest.genenames.org/fetch/symbol/%s"; //get Entrez ID for Sparql query
    private final String URL_PHARMGKB_GET_ID = "https://api.pharmgkb.org/v1/data/gene?symbol=%s";
    private final String URL_PHARMGKB_GET_CROSS_REFERENCE = "https://api.pharmgkb.org/v1/data/gene/%s/crossReferences"; //Insert pharmgkb id

    public final String URL_DISGENET = "http://rdf.disgenet.org/sparql/?default-graph-uri=&query=%s";

    /**
     * Currently limitig to 50 disease for performance
     */
    private String URL_SPARQL_QUERY_DISEASE_FOR_GENE = "SELECT DISTINCT ?disease FROM <http://rdf.disgenet.org> WHERE { ?gda <http://semanticscience.org/resource/SIO_000628>?gene,?disease . ?gene rdf:type <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C16612> . FILTER regex(?gene, \"%s$\", \"i\"). ?disease rdf:type <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C7057>.}";

    private final String URL_SPARQL_QUERY_DISEASE_GENE_CORRELATION_SCORE = "SELECT DISTINCT ?gda ?disease ?gene ?score ?source ?associationType ?pmid ?sentence FROM <http://rdf.disgenet.org> WHERE { ?gda <http://semanticscience.org/resource/SIO_000628> ?disease,?gene . ?disease rdf:type <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C7057> . FILTER regex(?disease, \"http://linkedlifedata.com/resource/umls/id/%1$s\") . ?gene rdf:type<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C16612> . FILTER regex(?gene, \"http://identifiers.org/ncbigene/%2$s\") . ?gda rdf:type ?associationType . ?gda <http://semanticscience.org/resource/SIO_000216> ?scoreIRI . ?scoreIRI <http://semanticscience.org/resource/SIO_000300> ?score . ?gda <http://semanticscience.org/resource/SIO_000253> ?source . OPTIONAL { ?gda <http://semanticscience.org/resource/SIO_000772> ?pmid . ?gda <http://purl.org/dc/terms/description> ?sentence . } } limit 1 ";

    private static QueryAlgorithm instance = new QueryAlgorithm();
    QueryInterface queryInterface = this;

    private HashMap<String,Object> map = new HashMap<>();

    public int diseaseCount;
    public int iterationCount;
    public long startTime;
    public long runTime;

    public ThreadPoolExecutor mDecodeThreadPool;

    public static QueryAlgorithm getInstance(){
        return instance;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loading);

        Bundle extras = getIntent().getExtras();
        String gen = null;
        int limit = 0;
        if(extras == null) {
          log("No data for algorithm");
        } else {
            gen = extras.getString("gene");
            limit = extras.getInt("limit");
        }

        if(gen!=null && limit > 0){
            Start(gen,limit);
        }
    }

    public void startLoadingScreen(final String geneName,final  int diseaseNum, final String geneDesc){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView gene = (TextView)findViewById(R.id.loading_gene_name);
                TextView diseaseCount = (TextView)findViewById(R.id.disease_text);
                gene.setText(geneName);
                TextView geneDescription = (TextView)findViewById(R.id.loading_gene_description);
                if (geneDesc != null && geneDescription.getText().toString().contains("calculating...")) {
                    geneDescription.setText(geneDesc);
                }

                if (diseaseNum > 0) {
                    diseaseCount.setText("Potential diseases found: \n" + diseaseNum);
                }else{
                    diseaseCount.setText("calculating...");
                }

            }
        });
    }

    public void updateLoader(final double current, final double max){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try{
                    double percentage = (current/max)*100;
                    int percInt = (int) percentage;
                    String perString = percInt+"%";
                    TextView percent = (TextView)findViewById(R.id.loading_text);
                    percent.setText(perString);

                    /**
                     * Smarteyeglass
                     */
                    SmarteyeglassUtils.getInstance().updateLayout(getApplicationContext(), "Completed: " + perString);

                }catch (NullPointerException e){}
            }
        });
    }
    public void Start(String gene, int limit){
        gene = gene.trim();
        if(limit<0 || limit >300){
            limit = 300;
        }
        startTime = System.currentTimeMillis();
        LIMIT = limit;

        log("Starting Algorithm for gene " + gene);
        mDecodeThreadPool = new ThreadPoolExecutor(
                50,       // Initial pool size
                100,       // Max pool size
                5, //time limit for thread
                TimeUnit.MINUTES, //timeunit
                new LinkedBlockingQueue<Runnable>());
        String url_formatted = String.format(URL_HGNC, gene);
        HttpHandler.getInstance().ReqeustGET(queryInterface, url_formatted, ENDPOINT_HGNC, gene);
        startLoadingScreen(gene, 0, null);
        showToast("Starting algorithm");
    }

    public void onTaskCompleted(JSONObject json, String endpoint, final String gene){
         switch (endpoint){
             case (ENDPOINT_DGIDB):
                 try{
                     JSONArray matchedTerms = json.getJSONArray("matchedTerms");
                     String geneLongName = iterateJSONArray(matchedTerms, "geneLongName");
                     map.put("geneLongName", geneLongName);
                     showToast("DGIDB response success");
                 }catch(Exception e){
                     e.printStackTrace();
                 }
                 break;
             case(ENDPOINT_HGNC):
                 try{
                     JSONObject response = json.getJSONObject("response");
                     JSONArray docs = response.getJSONArray("docs");
                     String entrez_id = iterateJSONArray(docs, "entrez_id");
                     String gen_desc = iterateJSONArray(docs, "name");
                     map.put("entrez_id", entrez_id);
                     String url_formatted = String.format(URL_SPARQL_QUERY_DISEASE_FOR_GENE, entrez_id);
                     String url_encoded = URLEncoder.encode(url_formatted, "UTF-8");
                     String url_complete = String.format(URL_DISGENET, url_encoded);
                     HttpHandler.getInstance().ReqeustGET(queryInterface, url_complete, ENDPOINT_DISGENET1, gene);

                     startLoadingScreen(gene, 0, gen_desc);
                     showToast("HGNC response success");
                     /**
                      * Smarteyeglass
                      */
                     SmarteyeglassUtils.getInstance().updateLayout(getApplicationContext(), gen_desc);

                 }catch (Exception e){
                     e.printStackTrace();
                 }
                 break;
             case(ENDPOINT_DISGENET1):
                 try{
                     JSONObject results = json.getJSONObject("results");
                     JSONArray bindings = results.getJSONArray("bindings");
                     HashMap<String, Object> diseases = new HashMap<String, Object>();
                     List<String> list = new ArrayList<String>();

                     for(int i=0; i<bindings.length() ; i++)
                     {
                         if(i<LIMIT){
                             JSONObject binding_in = (JSONObject)bindings.get(i);
                             JSONObject disease = (JSONObject)binding_in.get("disease");
                             String valueURL = disease.getString("value");
                             String value = valueURL.substring(valueURL.indexOf("id/") + 3);
                             diseases.put(value, 0);
                             list.add(value);
                         }
                     }
                     startLoadingScreen(gene, bindings.length(), null);

                     diseaseCount = list.size();
                     map.put("diseases", diseases);
                     final String url_formatted = String.format(URL_SPARQL_QUERY_DISEASE_GENE_CORRELATION_SCORE, "%s", map.get("entrez_id"));

                     for(int i=0; i<diseaseCount; i++){
                         final String disease_id = list.get(i);
                         Thread thread = new Thread() {
                             @Override
                             public void run() {
                                 log("Started new thread for disease " + disease_id);
                                 log("Active threads: " + Thread.activeCount());
                                 HttpHandler.getInstance().ReqeustGET_Multiple(queryInterface, url_formatted, disease_id , ENDPOINT_DISGENET2, gene);
                             }
                         };mDecodeThreadPool.submit(thread);
                     }
                     showToast("DISGENET response success");

                 }catch (JSONException e) {
                     e.printStackTrace();
                 }
                 break;
         }
    }
    public void onSubTaskCompleted(JSONObject json, String endpoint, String gene, String disease){
        showToast("Processing results");
        String value = "";
        try{
            JSONObject results = json.getJSONObject("results");
            JSONArray bindings = results.getJSONArray("bindings");
            boolean isDone = false;
            for(int i=0; i<bindings.length(); i++)
            {
                if(!isDone){
                    JSONObject binding_in = (JSONObject)bindings.get(i);
                    JSONObject disease_obj = (JSONObject)binding_in.get("score");
                    value = disease_obj.getString("value");
                    HashMap<String, Object> diseases_hash = (HashMap<String,Object>)map.get("diseases");
                    diseases_hash.put(disease, value);
                    map.put("diseases", diseases_hash);
                    log("Completed iteration " + iterationCount + " out of " +diseaseCount);
                    isDone = true;
                }
            }

        }catch (JSONException e) {
            iterationCount++;
            updateLoader(iterationCount, diseaseCount);
            e.printStackTrace();
        }
        iterationCount++;
        updateLoader(iterationCount, diseaseCount);

        if(iterationCount+1>=diseaseCount){
            queryInterface.onAlgCompleted(gene);
        }
        log("Exiting disease " + disease + " with value " +value+". Iterations: "+iterationCount);
    }


    public String iterateJSONArray(JSONArray json, String data){
        String output = "";
        try{
            for(int i=0; i<json.length(); i++)
            {
                JSONObject obj=json.getJSONObject(i);
                output = obj.getString(data);
            }
        }catch (JSONException e) {
            e.printStackTrace();
        }
        return output;
    }

    public void onAlgCompleted(final String gene){
        if(gene.equals("Timeout")){
            showToast("SPARQL Servers are down, cannot complete algorithm.");
            Intent intent = new Intent(getApplicationContext(), DoctorMain.class);
            startActivity(intent);
        }
        runTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
        log("RUNTIME : " + runTime + "seconds");

        /**
         * Smarteyeglass
         */
        SmarteyeglassUtils.getInstance().updateLayout(getApplicationContext(), "Total runtime: " + runTime + " seconds");

        HashMap<String, Object> diseases = (HashMap<String, Object>)map.get("diseases");

        Iterator<Map.Entry<String,Object>> iter = diseases.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String,Object> entry = iter.next();
            if("0".equals(entry.getValue().toString())){
                iter.remove();
            }
        }
        List<Pair<Object, String>> sol;
        sol = Utils.sortHashByValues(diseases);

        String diseases_str = "";

        for(int i=0 ; i<5; i++){
            try{
                Pair pair = sol.get(i);
                String score = pair.first.toString();
                String disease = pair.second.toString();
                Float scoref = Float.parseFloat(score);
                score = String.format("%.5f", scoref);
                diseases_str = diseases_str + disease+",";
                diseases_str = diseases_str + score+",";
            }catch (Exception e){
                e.printStackTrace();}
        }
        log("Output : " +diseases_str);
        final String[] disease_sol = diseases_str.split(",");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setContentView(R.layout.algorithm_results);
                TextView header = (TextView) findViewById(R.id.alg_res_header);
                header.setText(gene);
                ListAdapter resultAdapter = new ResultAdapter(getApplicationContext(), disease_sol);
                ListView tauListView = (ListView) findViewById(R.id.algListView);
                tauListView.setAdapter(resultAdapter);
            }
        });

    }
    public void showToast(final String txt)
    {
        runOnUiThread(new Runnable() {
            public void run()
            {
                Toast.makeText(getApplicationContext(),txt, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
