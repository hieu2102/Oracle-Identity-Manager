package vn.bnh.oim.scheduledtasks.lookups;

import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.scheduler.vo.TaskSupport;
import vn.bnh.oim.utils.LookupUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ReconcileLookups extends TaskSupport {
    private final ODLLogger logger = ODLLogger.getODLLogger(ReconcileLookups.class.getName());
    private HashMap<String, Object> scheduledTaskInputParams;
    private String lookupTable;
    private String fileDir;
    private String delimiter;

    @Override
    public void execute(HashMap hashMap) throws Exception {
        this.scheduledTaskInputParams = hashMap;
        setAttributes();
        HashMap<String, String> entriesFromCsv = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(this.fileDir))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(this.delimiter);
                if (values.length == 2) {
                    entriesFromCsv.put(values[0].trim(), values[1].trim());
                }
            }
        }
        LookupUtils.updateLookupTable(this.lookupTable, entriesFromCsv);

    }

    @Override
    public HashMap getAttributes() {
        return null;
    }

    @Override
    public void setAttributes() {
        this.lookupTable = this.scheduledTaskInputParams.get("Lookup Table").toString();
        this.fileDir = this.scheduledTaskInputParams.get("File Directory").toString();
        this.delimiter = this.scheduledTaskInputParams.get("Value Delimiter").toString();
    }
}
