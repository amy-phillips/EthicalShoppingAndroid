
function colour_page(response) {
    // grab all the tables for all the product types and munge them into a big useful struct
    munged_tables=get_munged_tables(response);

    // find the products
    // search results
    document.querySelectorAll('.product_name').forEach( function( product_div ){
        if(!product_div.parentNode.querySelector('#es-moar-infos')) {
            // want to colour in the item or product
            var tile_content=product_div.parentNode;
            while(tile_content && tile_content.classList && !tile_content.classList.contains("estore_product_container")) {
                tile_content=tile_content.parentNode;
            }
            var css_class="es-boots-search-result";
            if(!tile_content) {
                console.log("Error: failed to find estore_product_container node for "+product_div.textContent.trim());
                return;
            } 
            colour_product(munged_tables, product_div, tile_content, css_class, true, product_div.textContent.trim());
        }
    });

    //viewing single product
    document.querySelectorAll('#estore_product_title').forEach( function( product_div ){
        if(!product_div.parentNode.querySelector('#es-moar-infos')) {
            // want to colour in the item or product
            var tile_content=product_div.parentNode;
            while(tile_content && tile_content.classList && !tile_content.classList.contains("row")) {
                tile_content=tile_content.parentNode;
            }
            var css_class="es-boots-search-result";
            if(!tile_content) {
                console.log("Error: failed to find row node for "+product_div.textContent.trim());
                return;
            } 
            colour_product(munged_tables, product_div, tile_content, css_class, true, product_div.textContent.trim());
        }
    });

    // basket
    document.querySelectorAll('.basketitem').forEach( function( product_div ){
        if(!product_div.parentNode.querySelector('#es-moar-infos')) {
            // is this an image?  if so skip it
            if(product_div.textContent.trim().length==0)
                return;
            // want to colour in the item or product
            var tile_content=product_div.parentNode;
            while(tile_content && tile_content.classList && !tile_content.classList.contains("row")) {
                tile_content=tile_content.parentNode;
            }
            var css_class="es-boots-search-result";
            if(!tile_content) {
                console.log("Error: failed to find row node for "+product_div.textContent.trim());
                return;
            } 
            colour_product(munged_tables, product_div, tile_content, css_class, true, product_div.textContent.trim());
        }
    });
}

function get_header_location() {
    return document.querySelector('#header');
}
