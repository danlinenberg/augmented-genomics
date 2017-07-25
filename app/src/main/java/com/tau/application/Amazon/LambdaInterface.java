package com.tau.application.Amazon;

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;
import com.tau.application.Doctor;
import com.tau.application.Patient;

/**
 * Created by dan on 10/12/2016.
 */

public interface LambdaInterface {

    @LambdaFunction
    String table_prepare(Patient patient);

    @LambdaFunction
    String verify_qr(Patient patient);

    @LambdaFunction
    String device_whitelist(Patient patient);

    @LambdaFunction
    String data_delete(Patient patient);

    @LambdaFunction
    String find_vip_genes(Doctor doctor);

    @LambdaFunction
    String is_vcf_exist(Patient patient);

    @LambdaFunction
    String set_access_control(Patient patient);
}
