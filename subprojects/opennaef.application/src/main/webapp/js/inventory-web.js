function toggle_summary() {
    if ($('td #summary').is(':hidden')) {
        $('td').each(function () {
            $('#summary', this).show();
            $('#all', this).hide();
        });
    } else {
        $('td').each(function () {
            $('#summary', this).hide();
            $('#all', this).show();
        });
    }
    ;
}
