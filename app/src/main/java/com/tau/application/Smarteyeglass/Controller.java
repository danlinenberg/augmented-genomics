/*
Copyright (c) 2011, Sony Mobile Communications Inc.
Copyright (c) 2014, Sony Corporation

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 * Neither the name of the Sony Mobile Communications Inc.
 nor the names of its contributors may be used to endorse or promote
 products derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tau.application.Smarteyeglass;

import android.content.Context;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlTouchEvent;
import com.tau.application.R;

public final class Controller extends ControlExtension {

    private static final int SMARTEYEGLASS_API_VERSION = 1;
    public final int width;
    public final int height;
    public final String HEADER_WAITING_CONNECTION = "Awaiting login...";


    public Controller(final Context context,
                      final String hostAppPackageName, final String message) {
        super(context, hostAppPackageName);
        width = context.getResources().getDimensionPixelSize(R.dimen.smarteyeglass_control_width);
        height = context.getResources().getDimensionPixelSize(R.dimen.smarteyeglass_control_height);


        ExtensionService.Object.SmartEyeglassControl = this;
        if (message != null) {
            showToast(message);
        } else {
            updateLayout(HEADER_WAITING_CONNECTION);
        }
    }
    /**
     * Process Touch events.
     */
    @Override
    public void onTouch(final ControlTouchEvent event) {
        super.onTouch(event);
        //send message to main activity
//        ExtensionService.Object
//                .sendMessageToActivity("Hello Activity");
    }

    // Update the SmartEyeglass display when app becomes visible
    @Override
    public void onResume() {
//        updateLayout();
        super.onResume();
    }

    /**
     *  Update the display with the dynamic message text.
     */
    private void updateLayout(String text) {
        showLayout(R.layout.layout, null);
        sendText(R.id.btn_update_this, text);
    }

    public void showToast(final String message) {
    }
    /**
     * Provides a public method for ExtensionService and Activity to call in
     * order to request start.
     */
    public void requestExtensionStart() {
        startRequest();
    }
}
