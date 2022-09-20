package vn.bnh.oim.scheduledtasks.lookups;

import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.scheduler.vo.TaskSupport;
import vn.bnh.oim.utils.LookupUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

public class ReconcileGenericLookup extends TaskSupport {
    private final ODLLogger logger = ODLLogger.getODLLogger(ReconcileLookups.class.getName());
    private HashMap<String, Object> params;
    private String lookupTable;
    private String fileDir;
    private String delimiter;

    @Override
    public void execute(HashMap hashMap) throws Exception {
        this.params = hashMap;
        this.setAttributes();
        HashMap<String, String> lookupContent = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(this.fileDir))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(this.delimiter);
                if (values.length == 2) {
                    String key = values[0];
                    String value = values[1];
                    lookupContent.put(key, value);
                }
            }
            LookupUtils.updateLookupTable(this.lookupTable, lookupContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public HashMap getAttributes() {
        return null;
    }

    @Override
    public void setAttributes() {
        this.lookupTable = params.get("Lookup Table").toString();
        this.fileDir = params.get("File Dir").toString();
        this.delimiter = params.get("Value Delimiter").toString();

    }
}
