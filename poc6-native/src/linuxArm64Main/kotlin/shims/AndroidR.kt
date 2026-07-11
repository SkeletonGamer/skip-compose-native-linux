// POC 6 Jalon 4 : android.R.string (ids de ressources système Android) que SkipUI passe a stringResource.
// N'importe quel Int compile ; les valeurs reelles importent peu pour la cale.
package android

object R {
    object string {
        const val cancel = 0x01040000
        const val ok = 0x01040001
        const val search_go = 0x01040002
        const val dialog_alert_title = 0x01040003
    }
}
