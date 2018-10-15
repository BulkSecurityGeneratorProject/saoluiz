import { NgModule } from '@angular/core';

import { SaoluizSharedLibsModule, JhiAlertComponent, JhiAlertErrorComponent } from './';

@NgModule({
    imports: [SaoluizSharedLibsModule],
    declarations: [JhiAlertComponent, JhiAlertErrorComponent],
    exports: [SaoluizSharedLibsModule, JhiAlertComponent, JhiAlertErrorComponent]
})
export class SaoluizSharedCommonModule {}
