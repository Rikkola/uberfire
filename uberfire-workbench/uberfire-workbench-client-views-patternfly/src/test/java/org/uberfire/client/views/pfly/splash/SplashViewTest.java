package org.uberfire.client.views.pfly.splash;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.uberfire.client.views.pfly.modal.Bs3Modal;

import com.github.gwtbootstrap.client.ui.event.HideHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;

@RunWith(GwtMockitoTestRunner.class)
public class SplashViewTest {

    private SplashViewUnitTestWrapper splashViewUnitTestWrapper;

    @Before
    public void setup() {
        splashViewUnitTestWrapper = new SplashViewUnitTestWrapper();
    }


    @Test
      public void setContentDelegationTest() {
        Bs3Modal modalPresenter = splashViewUnitTestWrapper.getModal();
        IsWidget mock = mock( IsWidget.class );
        int height = 1;
        splashViewUnitTestWrapper.setContent( mock, height );

        verify( modalPresenter ).add( mock );
        verify( modalPresenter ).add( splashViewUnitTestWrapper.footer );
        verify( modalPresenter ).setBodyHeigth( height );
    }

    @Test
    public void showDelegationTest() {
        Bs3Modal modalPresenter = splashViewUnitTestWrapper.getModal();
        splashViewUnitTestWrapper.show();

        verify( modalPresenter ).show();
        verify( modalPresenter ).addHideHandler( any( HideHandler.class ) );
    }

    @Test
    public void hideDelegationTest() {
        Bs3Modal modalPresenter = splashViewUnitTestWrapper.getModal();
        splashViewUnitTestWrapper.hide();

        verify( modalPresenter ).hide();
    }



}
