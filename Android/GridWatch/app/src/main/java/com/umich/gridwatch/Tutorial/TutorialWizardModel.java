package com.umich.gridwatch.Tutorial;

import android.content.Context;

import com.tech.freak.wizardpager.model.AbstractWizardModel;
import com.tech.freak.wizardpager.model.PageList;
import com.umich.gridwatch.Tutorial.Pages.Tutorial.Page1;
import com.umich.gridwatch.Tutorial.Pages.Tutorial.Page10;
import com.umich.gridwatch.Tutorial.Pages.Tutorial.Page11;
import com.umich.gridwatch.Tutorial.Pages.Tutorial.Page12;
import com.umich.gridwatch.Tutorial.Pages.Tutorial.Page2;
import com.umich.gridwatch.Tutorial.Pages.Tutorial.Page3;
import com.umich.gridwatch.Tutorial.Pages.Tutorial.Page4;
import com.umich.gridwatch.Tutorial.Pages.Tutorial.Page5;
import com.umich.gridwatch.Tutorial.Pages.Tutorial.Page6;
import com.umich.gridwatch.Tutorial.Pages.Tutorial.Page7;
import com.umich.gridwatch.Tutorial.Pages.Tutorial.Page8;
import com.umich.gridwatch.Tutorial.Pages.Tutorial.Page9;

/**
 * Created by nklugman on 5/20/15.
 */
public class TutorialWizardModel extends AbstractWizardModel {
    public TutorialWizardModel(Context context) {
        super(context);
    }

    @Override
    protected PageList onNewRootPageList() {
        return new PageList(
                new Page1(this, "Tutorial 1 of 12").setRequired(true),
                new Page2(this, "Tutorial 2 of 12").setRequired(true),

                new Page3(this, "Tutorial 3 of 12").setRequired(true),
                new Page4(this, "Tutorial 4 of 12").setRequired(true),
                new Page5(this, "Tutorial 5 of 12").setRequired(true),
                new Page6(this, "Tutorial 6 of 12").setRequired(true),
                new Page7(this, "Tutorial 7 of 12").setRequired(true),
                new Page8(this, "Tutorial 8 of 12").setRequired(true),
                new Page9(this, "Tutorial 9 of 12").setRequired(true),
                new Page10(this, "Tutorial 10 of 12").setRequired(true),
                new Page11(this, "Tutorial 11 of 12").setRequired(true),
                new Page12(this, "Tutorial 12 of 12").setRequired(true)


        );
    }
}

                /*
                new BranchPage(this, "Welcome! Do You Want A Quick Introduction to GridWatch?")
                        .addBranch(
                                "Yes",
                                new Page1(this, "Tutorial 1 of 12").setRequired(true),
                                new Page2(this, "Tutorial 2 of 12").setRequired(true),
                                new Page3(this, "Tutorial 3 of 12").setRequired(true),
                                new Page4(this, "Tutorial 4 of 12").setRequired(true),
                                new Page5(this, "Tutorial 5 of 12").setRequired(true),
                                new Page6(this, "Tutorial 6 of 12").setRequired(true),
                                new Page7(this, "Tutorial 7 of 12").setRequired(true),
                                new Page8(this, "Tutorial 8 of 12").setRequired(true),
                                new Page9(this, "Tutorial 9 of 12").setRequired(true),
                                new Page10(this, "Tutorial 10 of 12").setRequired(true),
                                new Page11(this, "Tutorial 11 of 12").setRequired(true),
                                new Page12(this, "Tutorial 12 of 12").setRequired(true)
                        ).addBranch(
                                "No"
                                */
                        /*
                                new BranchPage(this, "Login")
                              .addBranch(
                        "I Need An Account",
                        new NewAccountPage(this, "Create New Account").setRequired(true),
                        new OptionalAccountPage(this, "Some Additional Information").setRequired(true),
                        new BranchPage(this, "Are You Part of a Beta Trial?")
                                .addBranch(
                                        "Yes",
                                        new BetaPage(this, "Enter In Beta Information").setRequired(true))
                                .addBranch("No")
                                .setValue("No"))
                .addBranch(
                        "I Have An Account",
                        new LoginPage(this, "Login").setRequired(true))
                 .addBranch(
                                "Skip This"

                        )
                        */
        //));
    //}
//}