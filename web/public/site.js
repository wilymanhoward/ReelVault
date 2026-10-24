(() => {
  const collections = {
    weekend: ['The weekend list', 'The hike someone sent you. That tiny bookshop. A day worth getting out for.'],
    kitchen: ['Something worth making', 'The five-minute lunch. A family dinner idea. That recipe you keep meaning to try.'],
    ideas: ['Keep that thought', 'A clever tutorial. A project for a rainy day. An idea you don’t want to lose.'],
  };
  document.querySelectorAll('[data-collection]').forEach(button => {
    button.addEventListener('click', () => {
      document.querySelectorAll('[data-collection]').forEach(tab => tab.setAttribute('aria-pressed', String(tab === button)));
      const [title, description] = collections[button.dataset.collection];
      document.getElementById('demo-title').textContent = title;
      document.getElementById('demo-description').textContent = description;
    });
  });
})();
