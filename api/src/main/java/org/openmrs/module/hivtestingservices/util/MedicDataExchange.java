package org.openmrs.module.hivtestingservices.util;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.ValueNode;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.hivtestingservices.api.HTSService;
import org.openmrs.module.hivtestingservices.api.service.DataService;
import org.openmrs.module.hivtestingservices.api.service.MedicQueData;
import org.openmrs.module.hivtestingservices.model.DataSource;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MedicDataExchange {
    HTSService htsService = Context.getService(HTSService.class);
    DataService dataService = Context.getService(DataService.class);
    static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");


    /**
     * processes results from cht     *
     * @param resultPayload this should be an object
     * @return
     */
    public String processIncomingMedicDataQueue(String resultPayload) {
        Integer statusCode;
        String statusMsg;
        String json = null;
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonNodes = null;


        ObjectNode jsonNode = null;
        try {
            jsonNode = (ObjectNode) mapper.readTree(resultPayload);

        } catch (IOException e) {
            e.printStackTrace();
        }
        if (jsonNode != null) {

            ObjectNode formNode =  processFormsPayload(jsonNode);
            String payload = formNode.toString();


            String discriminator = formNode.path("discriminator").path("discriminator").getTextValue();
            String formDataUuid = formNode.path("encounter").path("encounter.form_uuid").getTextValue();
            String patientUuid = formNode.path("patient").path("patient.uuid").getTextValue();
            Integer locationId = Integer.parseInt(formNode.path("encounter").path("encounter.location_id").getTextValue());
            String providerString = formNode.path("encounter").path("encounter.provider_id").getTextValue();

            saveMedicDataQueue(payload,locationId,providerString,patientUuid,discriminator,formDataUuid);

        }
        return "Data queue created successfully";
    }

    public String processIncomingRegistration(String resultPayload) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonNode = null;
        try {
            jsonNode = (ObjectNode) mapper.readTree(resultPayload);

        } catch (IOException e) {
            e.printStackTrace();
        }
        if (jsonNode != null) {
            ObjectNode registrationNode = processRegistrationPayload(jsonNode);
            String payload = registrationNode.toString();
            String discriminator = registrationNode.path("discriminator").path("discriminator").getTextValue();
            String formDataUuid = registrationNode.path("encounter").path("encounter.form_uuid").getTextValue();
            String patientUuid = registrationNode.path("patient").path("patient.uuid").getTextValue();
            Integer locationId = Integer.parseInt(registrationNode.path("encounter").path("encounter.location_id").getTextValue());
            String providerString = registrationNode.path("encounter").path("encounter.provider_id").getTextValue();

            saveMedicDataQueue(payload,locationId,providerString,patientUuid,discriminator,formDataUuid);

        }
        return "Data queue registration created successfully";
    }

    private void saveMedicDataQueue(String payload, Integer locationId, String providerString, String patientUuid, String discriminator,
                                    String formUuid) {
        DataSource dataSource = dataService.getDataSource(1);
        Provider provider = Context.getProviderService().getProviderByIdentifier(providerString);
        Location location = Context.getLocationService().getLocation(locationId);
        Form form = Context.getFormService().getFormByUuid(formUuid);

        MedicQueData medicQueData = new MedicQueData();
        if(form !=null && form.getName() !=null) { medicQueData.setFormName(form.getName());
        }else {
            medicQueData.setFormName("Unknown name");
        }
        medicQueData.setPayload(payload);
        medicQueData.setDiscriminator(discriminator);
        medicQueData.setPatientUuid(patientUuid);
        medicQueData.setFormDataUuid(formUuid);
        medicQueData.setProvider(provider);
        medicQueData.setLocation(location);
        medicQueData.setDataSource(dataSource);
        htsService.saveQueData(medicQueData);

    }

    private ObjectNode processRegistrationPayload (ObjectNode jsonNode) {

        ObjectNode patientNode = JsonNodeFactory.instance.objectNode();
        ObjectNode obs = JsonNodeFactory.instance.objectNode();
        ObjectNode tmp = JsonNodeFactory.instance.objectNode();
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        ObjectNode encounter = JsonNodeFactory.instance.objectNode();
        ObjectNode identifier = JsonNodeFactory.instance.objectNode();
        ObjectNode registrationWrapper = JsonNodeFactory.instance.objectNode();
        identifier.put("identifier_type_name","National ID");
        identifier.put("identifier_value",jsonNode.get("patient_nationalIdnumber").getTextValue());
        identifier.put("confirm_other_identifier_value",jsonNode.get("patient_nationalIdnumber").getTextValue());

        patientNode.put("patient.uuid",jsonNode.get("_id").getTextValue());
        patientNode.put("patient.family_name",jsonNode.get("patient_familyName").getTextValue());
        patientNode.put("patient.given_name",jsonNode.get("patient_firstName").getTextValue());
        patientNode.put("patient.middle_name",jsonNode.get("patient_middleName").getTextValue());
        // patientNode.put("patient.mothers_name",jsonNode.get("patient_familyName").getTextValue());
        // patientNode.put("patient.medical_record_number","337");
        patientNode.put("patient.sex",gender(jsonNode.get("patient_sex").getTextValue()));
        patientNode.put("patient.birth_date",formatStringDate(jsonNode.get("patient_birthDate").getTextValue()));
        // patientNode.put("patient.birthdate_estimated",jsonNode.get("patient_familyName").getTextValue());
        patientNode.put("patient.county",jsonNode.get("patient_county").getTextValue());
        patientNode.put("patient.sub_county",jsonNode.get("patient_subcounty").getTextValue());
        patientNode.put("patient.ward",jsonNode.get("patient_ward").getTextValue());
        patientNode.put("patient.village",jsonNode.get("patient_village").getTextValue());
        patientNode.put("patient.landmark",jsonNode.get("patient_landmark").getTextValue());
        patientNode.put("patient.phone_number",jsonNode.get("patient_telephone").getTextValue());
        patientNode.put("patient.alternate_phone_contact",jsonNode.get("patient_alternatePhone").getTextValue());
        patientNode.put("patient.postal_address",jsonNode.get("patient_alternatePhone").getTextValue());
        patientNode.put("patient.next_of_kin_name",jsonNode.get("patient_nextofkin").getTextValue());
        patientNode.put("patient.next_of_kin_relationship",jsonNode.get("patient_nextofkinRelationship").getTextValue());
        patientNode.put("patient.next_of_kin_contact",jsonNode.get("patient_nextOfKinPhonenumber").getTextValue());
        patientNode.put("patient.next_of_kin_address",jsonNode.get("patient_nextOfKinPostaladdress").getTextValue());
        patientNode.put("patient.otheridentifier",identifier);

        obs.put("identifier_type_name","National ID");
        obs.put("1054^CIVIL STATUS^99DCT",jsonNode.get("_1054_maritalStatus_99DCT").getTextValue().replace("_","^").substring(1));
        obs.put("1542^OCCUPATION^99DCT",jsonNode.get("_1542_occupation_99DCT").getTextValue().replace("_","^").substring(1));
        obs.put("1712^HIGHEST EDUCATION LEVEL^99DCT",jsonNode.get("_1712_education_99DCT").getTextValue().replace("_","^").substring(1));

        tmp.put("tmp.birthdate_type","age");
        tmp.put("tmp.age_in_years",jsonNode.get("patient_ageYears").getTextValue());
        discriminator.put("discriminator","json-registration");

        encounter.put("encounter.location_id","7185");
        encounter.put("encounter.provider_id_select","admin");
        encounter.put("encounter.provider_id","admin");
        encounter.put("encounter.encounter_datetime",convertTime(jsonNode.get("reported_date").getLongValue()));
        encounter.put("encounter.form_uuid","8898c6e1-5df1-409f-b8ed-c88e6e0f24e9");
        encounter.put("encounter.user_system_id",jsonNode.path("meta").path("created_by").getTextValue());
        encounter.put("encounter.device_time_zone","Africa\\/Nairobi");
        encounter.put("encounter.setup_config_uuid","2107eab5-5b3a-4de8-9e02-9d97bce635d2");

        registrationWrapper.put("patient",patientNode);
        registrationWrapper.put("observation",obs);
        registrationWrapper.put("tmp",tmp);
        registrationWrapper.put("discriminator",discriminator);
        registrationWrapper.put("encounter",encounter);

        return registrationWrapper;
    }

    private ObjectNode processFormsPayload (ObjectNode jsonNode) {
        ObjectNode formsNode = JsonNodeFactory.instance.objectNode();
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        ObjectNode encounter = JsonNodeFactory.instance.objectNode();
        ObjectNode patientNode = JsonNodeFactory.instance.objectNode();




        ObjectMapper mapper = new ObjectMapper();
        ObjectNode obsNodes = null;
        ObjectNode jsonNodes = null;
        String json = null;
        try {
            jsonNodes = (ObjectNode) mapper.readTree(jsonNode.path("fields").path("observation").toString());
            json = new ObjectMapper().writeValueAsString(jsonNodes);
            if(json != null) {
                obsNodes = (ObjectNode) mapper.readTree(json.replace("_","^"));
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        discriminator.put("discriminator","json-encounter");
        encounter.put("encounter.location_id","7185");
        encounter.put("encounter.provider_id_select","admin");
        encounter.put("encounter.provider_id","admin");
        encounter.put("encounter.encounter_datetime",convertTime(jsonNode.get("reported_date").getLongValue()));
        encounter.put("encounter.form_uuid","402dc5d7-46da-42d4-b2be-f43ea4ad87b0");
        encounter.put("encounter.user_system_id","admin");
        encounter.put("encounter.device_time_zone","Africa\\/Nairobi");
        encounter.put("encounter.setup_config_uuid","9c0a7a57-62ff-4f75-babe-5835b0e921b7");

        patientNode.put("patient.uuid","a4de1e16-da72-11ea-87d0-0242ac130003");
        patientNode.put("patient.family_name","Joo");
        patientNode.put("patient.given_name","Joo");
        patientNode.put("patient.middle_name","Joo");
        patientNode.put("patient.sex",gender("female"));
        patientNode.put("patient.birth_date","02-01-1989");


        if(obsNodes != null){
            Iterator<Map.Entry<String,JsonNode>> iterator = obsNodes.getFields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                if(entry.getKey().contains("Multi")) {
                    obsNodes.put(entry.getKey(), handleMultiSelectFields(entry.getValue().toString().replace(" ",",")));

                }


            }

        }
        formsNode.put("patient", patientNode);
        formsNode.put("observation", obsNodes);
        formsNode.put("discriminator",discriminator);
        formsNode.put("encounter",encounter);

        return   formsNode;

    }

    private ArrayNode handleMultiSelectFields(String listOfItems){
        ArrayNode arrNode = JsonNodeFactory.instance.arrayNode();
        if (listOfItems !=null) {
            for (String s : listOfItems.split(",")) {
                arrNode.add(s.substring(1,s.length()-1));
            }
        }
        return arrNode;
    }

    private String gender(String gender) {
        String abbriviateGender = null;
        if(gender.equalsIgnoreCase("male")){
            abbriviateGender ="M";
        }
        if(gender.equalsIgnoreCase("female")) {
            abbriviateGender ="F";
        }
        return abbriviateGender;
    }

    private  String formatStringDate(String dob)  {
        String date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
            date =sdf.format(sdf2.parse(dob));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    private String convertTime(long time){
        Date date = new Date(time);
        return DATE_FORMAT.format(date);
    }



}
